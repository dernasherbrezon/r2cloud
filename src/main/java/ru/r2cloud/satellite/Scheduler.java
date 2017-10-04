package ru.r2cloud.satellite;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.Lifecycle;
import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Scheduler implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final SatelliteDao satellites;
	private final Configuration config;
	private final File basepath;
	private final Predict predict;
	private ScheduledExecutorService executor = null;
	private final RtlSdrLock lock;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock, Predict predict) {
		this.config = config;
		this.config.subscribe(this);
		this.satellites = satellites;
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.lock = lock;
		this.predict = predict;
	}

	@Override
	public void onConfigUpdated() {
		boolean satellitesEnabled = config.getBoolean("satellites.enabled");
		if (executor == null && satellitesEnabled) {
			start();
		} else if (executor != null && !satellitesEnabled) {
			stop();
		}

	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (!config.getBoolean("satellites.enabled")) {
			LOG.info("satellite scheduler is disabled");
			return;
		}
		if (executor != null) {
			return;
		}
		executor = Executors.newScheduledThreadPool(2, new NamingThreadFactory("scheduler"));
		for (ru.r2cloud.model.Satellite cur : satellites.findSupported()) {
			TLE tle = new TLE(new String[] { cur.getName(), cur.getTleLine1(), cur.getTleLine2() });
			Satellite satellite = SatelliteFactory.createSatellite(tle);
			schedule(cur, satellite);
		}

		LOG.info("started");
	}

	private void schedule(ru.r2cloud.model.Satellite cur, Satellite satellite) {
		long current = System.currentTimeMillis();
		SatPass nextPass = predict.calculateNext(new Date(current), satellite);
		if (nextPass == null) {
			LOG.info("can't find next pass for " + cur.getName());
			cur.setNextPass(null);
			return;
		}
		LOG.info("scheduled next pass for " + cur.getName() + ": " + nextPass);
		cur.setNextPass(nextPass.getStart().getTime());
		// ./data/satellites/12312/data/1234234
		File basepathForCurrent = new File(basepath, cur.getId() + File.separator + "data" + File.separator + System.currentTimeMillis());
		Future<?> future = executor.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				execute(basepathForCurrent, cur);
			}
		}, nextPass.getStart().getTime().getTime() - current, TimeUnit.MILLISECONDS);
		executor.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				schedule(cur, satellite);

				File wavPath = new File(basepathForCurrent, "output.wav");
				if (!wavPath.exists()) {
					LOG.info("nothing saved. cleanup current directory: " + basepathForCurrent.getAbsolutePath());
					if (!basepathForCurrent.delete()) {
						LOG.error("unable to delete current base directory: " + basepathForCurrent.getAbsolutePath());
					}
					return;
				}

				processSource(wavPath, "a");
				processSource(wavPath, "b");
				if (!wavPath.delete()) {
					LOG.error("unable to delete source .wav: " + wavPath.getAbsolutePath());
				}

				File[] dataDirs = basepathForCurrent.getParentFile().listFiles();
				Integer maxCount = config.getInteger("scheduler.data.retention.count");
				if (dataDirs.length > maxCount) {
					Arrays.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
					for (int i = 0; i < (dataDirs.length - maxCount); i++) {
						Util.deleteDirectory(dataDirs[i]);
					}
				}

				// FIXME send image somewhere

			}
		}, nextPass.getEnd().getTime().getTime() - current, TimeUnit.MILLISECONDS);
	}

	private void processSource(final File wavFile, String type) {
		File result = new File(wavFile.getParentFile(), type + ".jpg");
		String[] cmd = new String[] { config.getProperty("satellites.wxtoimg.path"), "-t", "n", "-" + type, "-c", "-o", wavFile.getAbsolutePath(), result.getAbsolutePath() };
		Process process = null;
		try {
			process = new ProcessBuilder().inheritIO().command(cmd).inheritIO().start();
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		}
	}

	// put synchonized so no 2 observations run at the same time
	private synchronized void execute(File basepathForCurrent, ru.r2cloud.model.Satellite satellite) {
		File wavPath = new File(basepathForCurrent, "output.wav");
		Process process = null;
		if (!lock.tryLock(this)) {
			LOG.info("unable to acquire lock for " + satellite.getName());
			return;
		}
		try {
			process = new ProcessBuilder().command(new String[] { config.getProperty("satellites.rtlsdr.path"), "-f", String.valueOf(satellite.getFrequency()), "-s", "60k", "-g", "45", "-p", "55", "-E", "wav", "-E", "deemp", "-F", "9", "-", "|", config.getProperty("satellites.sox.path"), "-t", "wav", "-", wavPath.getAbsolutePath(), "rate", "11025" }).inheritIO().start();
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("rtl_sdr for satellites", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} finally {
			lock.unlock(this);
		}
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
		LOG.info("stopped");
	}

}

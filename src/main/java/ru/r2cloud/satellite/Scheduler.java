package ru.r2cloud.satellite;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
	private static final int BUF_SIZE = 0x1000; // 4K

	private final SatelliteDao satellites;
	private final Configuration config;
	private final File basepath;
	private final Predict predict;
	private ScheduledExecutorService scheduler = null;
	private ScheduledExecutorService reaper = null;
	private final RtlSdrLock lock;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock, Predict predict) {
		this.config = config;
		this.config.subscribe(this, "satellites.enabled");
		this.satellites = satellites;
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.lock = lock;
		this.predict = predict;
	}

	@Override
	public void onConfigUpdated() {
		boolean satellitesEnabled = config.getBoolean("satellites.enabled");
		if (scheduler == null && satellitesEnabled) {
			start();
		} else if (scheduler != null && !satellitesEnabled) {
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
		if (scheduler != null) {
			return;
		}
		List<ru.r2cloud.model.Satellite> supportedSatellites = satellites.findSupported();
		scheduler = Executors.newScheduledThreadPool(1, new NamingThreadFactory("scheduler"));
		reaper = Executors.newScheduledThreadPool(1, new NamingThreadFactory("reaper"));
		for (ru.r2cloud.model.Satellite cur : supportedSatellites) {
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
		File outputDirectory = new File(basepath, cur.getId() + File.separator + "data" + File.separator + cur.getNextPass().getTime());
		Future<?> future = scheduler.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				if (!outputDirectory.mkdirs()) {
					LOG.info("unable to create output directory: " + outputDirectory.getAbsolutePath());
					return;
				}
				execute(outputDirectory, cur);
			}
		}, nextPass.getStart().getTime().getTime() - current, TimeUnit.MILLISECONDS);
		reaper.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				schedule(cur, satellite);

				File wavPath = new File(outputDirectory, "output.wav");
				if (!wavPath.exists()) {
					LOG.info("nothing saved. cleanup current directory: " + outputDirectory.getAbsolutePath());
					if (!outputDirectory.delete()) {
						LOG.error("unable to delete current base directory: " + outputDirectory.getAbsolutePath());
					}
					return;
				}

				processSource(wavPath, "a");
				processSource(wavPath, "b");
				if (!wavPath.delete()) {
					LOG.error("unable to delete source .wav: " + wavPath.getAbsolutePath());
				}

				File[] dataDirs = outputDirectory.getParentFile().listFiles();
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
	private synchronized void execute(File outputDirectory, ru.r2cloud.model.Satellite satellite) {
		File wavPath = new File(outputDirectory, "output.wav");
		if (!lock.tryLock(this)) {
			LOG.info("unable to acquire lock for " + satellite.getName());
			return;
		}
		Process rtlfm = null;
		Process sox = null;
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			sox = new ProcessBuilder().command(new String[] { config.getProperty("satellites.sox.path"), "-t", "raw", "-r", "60000", "-es", "-b", "16", "-", wavPath.getAbsolutePath(), "rate", "11025" }).redirectError(Redirect.INHERIT).start();
			rtlfm = new ProcessBuilder().command(new String[] { config.getProperty("satellites.rtlsdr.path"), "-f", String.valueOf(satellite.getFrequency()), "-s", "60k", "-g", "45", "-p", String.valueOf(ppm), "-E", "deemp", "-F", "9", "-" }).redirectError(Redirect.INHERIT).start();
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = rtlfm.getInputStream().read(buf);
				if (r == -1) {
					break;
				}
				sox.getOutputStream().write(buf, 0, r);
			}
			sox.getOutputStream().flush();
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} finally {
			LOG.info("stopping pipe thread");
			Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
			Util.shutdown("sox", sox, 10000);
			lock.unlock(this);
		}
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		Util.shutdown(scheduler, config.getThreadPoolShutdownMillis());
		Util.shutdown(reaper, config.getThreadPoolShutdownMillis());
		scheduler = null;
		LOG.info("stopped");
	}

}

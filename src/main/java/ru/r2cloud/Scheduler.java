package ru.r2cloud;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Scheduler implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final SatelliteDao satellites;
	private final Configuration config;
	private final double minElevation;
	private final double guaranteedElevation;
	private final GroundStationPosition currentLocation;
	private final File basepath;
	private ScheduledExecutorService executor = null;
	private final RtlSdrLock lock;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock) {
		this.config = config;
		this.satellites = satellites;
		this.minElevation = config.getDouble("scheduler.elevation.min");
		this.guaranteedElevation = config.getDouble("scheduler.elevation.guaranteed");
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.lock = lock;
		// TODO doesnt support reloading coordinates
		this.currentLocation = new GroundStationPosition(config.getDouble("locaiton.lat"), config.getDouble("locaiton.lon"), 0.0);
	}

	//protection from calling start 2 times and more
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
		SatPass nextPass = findNext(new Date(current), satellite);
		if (nextPass == null) {
			LOG.info("can't find next pass for " + cur.getName());
			return;
		}
		LOG.info("scheduled next pass for " + cur.getName() + ": " + nextPass);
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
					Arrays.sort(dataDirs, FilenameComparator.INSTANCE);
					for (int i = 0; i < (dataDirs.length - maxCount); i++) {
						Util.deleteDirectory(dataDirs[i]);
					}
				}

				// FIXME send image somewhere

			}
		}, nextPass.getEnd().getTime().getTime() - current, TimeUnit.MILLISECONDS);
	}

	private static void processSource(final File wavFile, String type) {
		File result = new File(wavFile.getParentFile(), type + ".jpg");
		String[] cmd = new String[] { "wxtoimg", "-t", "n", "-" + type, "-c", "-o", wavFile.getAbsolutePath(), result.getAbsolutePath() };
		Process process = null;
		try {
			process = new ProcessBuilder().inheritIO().command(cmd).inheritIO().start();
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdownProcess(process, 10000);
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
			process = new ProcessBuilder().inheritIO().command(new String[] { config.getProperty("satellites.rtlsdr.path"), "-f", String.valueOf(satellite.getFrequency()), "-s", "60k", "-g", "45", "-p", "55", "-E", "wav", "-E", "deemp", "-F", "9", "-", "|", config.getProperty("satellites.sox.path"), "-t", "wav", "-", wavPath.getAbsolutePath(), "rate", "11025" }).inheritIO().start();
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdownProcess(process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} finally {
			lock.unlock(this);
		}
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		LOG.info("stopped");
	}

	// package protected for tests
	SatPass findNext(Date current, Satellite satellite) {
		if (!satellite.willBeSeen(currentLocation)) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(current);
		boolean matched = false;
		SatPos previous = null;
		SatPos start = null;
		double maxElevation = 0.0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 60; j++) {
				cal.add(Calendar.MINUTE, 1);
				SatPos position = satellite.getPosition(currentLocation, cal.getTime());
				double elevation = elevation(position);
				if (elevation >= minElevation) {
					// calculate max elevation only during satellite pass
					maxElevation = Math.max(maxElevation, elevation);
					if (!matched) {
						start = findPrecise(previous, position, satellite);
					}
					matched = true;
				} else {
					if (matched) {
						if (maxElevation >= guaranteedElevation) {
							SatPass result = new SatPass();
							result.setStart(start);
							result.setEnd(findPrecise(previous, position, satellite));
							return result;
						}
					}
					matched = false;
				}
				previous = position;
			}
		}
		return null;
	}

	// log(n) binary search of visible pass. precision is 1 second
	private SatPos findPrecise(SatPos start, SatPos end, Satellite satellite) {
		long middle = (end.getTime().getTime() / TimeUnit.SECONDS.toMillis(1) - start.getTime().getTime() / TimeUnit.SECONDS.toMillis(1)) / 2;
		if (middle == 0) {
			if (elevation(end) >= minElevation) {
				return end;
			} else {
				return start;
			}
		}
		SatPos newEnd = satellite.getPosition(currentLocation, new Date(start.getTime().getTime() + middle * TimeUnit.SECONDS.toMillis(1)));
		boolean isMiddleVisible = elevation(newEnd) >= minElevation;
		// return left of right part of timeline
		if (elevation(end) >= minElevation) {
			if (isMiddleVisible) {
				return findPrecise(start, newEnd, satellite);
			} else {
				return findPrecise(newEnd, end, satellite);
			}
		} else {
			if (isMiddleVisible) {
				return findPrecise(newEnd, end, satellite);
			} else {
				return findPrecise(start, newEnd, satellite);
			}
		}
	}

	private static double elevation(SatPos sat) {
		return sat.getElevation() / (Math.PI * 2.0) * 360;
	}

}

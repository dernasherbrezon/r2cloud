package ru.r2cloud.satellite;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.PathStats;
import ru.r2cloud.util.Util;

public class TimeSizeRetention {

	private static final Logger LOG = LoggerFactory.getLogger(TimeSizeRetention.class);

	private final Map<String, PathStats> statsByPath = new HashMap<>();
	private final Map<String, PathStats> rawIqStats = new HashMap<>();
	private final long maxSize;
	private final int maxCountRawData;
	private long allObservationsSize = 0;

	public TimeSizeRetention(long maxSize, int maxCountRawData, Path basedir) {
		this.maxSize = maxSize;
		this.maxCountRawData = maxCountRawData;
		if (Files.exists(basedir)) {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(basedir)) {
				for (Path curSatellite : ds) {
					Path dataRoot = curSatellite.resolve("data");
					if (!Files.exists(dataRoot)) {
						// do not empty satellite directory
						// it might contain cached tle.txt file which is useful
						// when no internet connection present and new observation is about
						// to schedule
						continue;
					}
					try (DirectoryStream<Path> observationDirs = Files.newDirectoryStream(dataRoot)) {
						for (Path curObservation : observationDirs) {
							if (!Files.isDirectory(curObservation)) {
								continue;
							}
							indexAndCleanup(curObservation);
						}
					}
				}
			} catch (IOException e) {
				LOG.error("unable to index all", e);
			}
		}
	}

	public void indexAndCleanup(Path curObservation) {
		long minTime = Long.MAX_VALUE;
		long totalSize = 0;
		boolean withRawIq = false;
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(curObservation)) {
			for (Path file : dir) {
				FileTime time = Files.getLastModifiedTime(file);
				minTime = Math.min(minTime, time.toMillis());
				totalSize += Files.size(file);
				if (isRawIq(file)) {
					withRawIq = true;
				}
			}
		} catch (IOException e) {
			LOG.error("unable to index observation: {}", curObservation, e);
		}
		PathStats newStats = new PathStats();
		newStats.setLastUpdateTime(minTime);
		newStats.setPath(curObservation);
		newStats.setSize(totalSize);

		String key = curObservation.toString();
		synchronized (this) {
			PathStats oldStats = statsByPath.remove(key);
			if (oldStats != null) {
				allObservationsSize -= oldStats.getSize();
			}
			statsByPath.put(key, newStats);
			if (withRawIq) {
				rawIqStats.put(key, newStats);
			}
			allObservationsSize += newStats.getSize();
			while (rawIqStats.size() > maxCountRawData) {
				String minKey = findMin(rawIqStats);
				if (minKey == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				PathStats min = rawIqStats.remove(minKey);
				if (min == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				LOG.info("deleting IQ data from observation: {} last update time: {}", min.getPath(), new Date(min.getLastUpdateTime()));
				try (DirectoryStream<Path> dir = Files.newDirectoryStream(min.getPath())) {
					for (Path file : dir) {
						if (isRawIq(file)) {
							long filesize = Files.size(file);
							Files.delete(file);
							allObservationsSize -= filesize;
							// normally single raw IQ per observation
							break;
						}
					}
				} catch (IOException e) {
					LOG.error("unable to delete raw IQ file from: {}", curObservation, e);
				}
			}
			while (allObservationsSize > maxSize) {
				String minKey = findMin(statsByPath);
				if (minKey == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				PathStats min = statsByPath.remove(minKey);
				if (min == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				LOG.info("deleting old observation: {} last update time: {}", min.getPath(), new Date(min.getLastUpdateTime()));
				if (Util.deleteDirectory(min.getPath())) {
					allObservationsSize -= min.getSize();
				}
			}
		}

	}

	private static boolean isRawIq(Path file) {
		String filename = file.toString();
		return filename.contains(ObservationDao.OUTPUT_RAW_FILENAME) || filename.contains(ObservationDao.OUTPUT_RAW_FILENAME_GZIPPED) || filename.contains(ObservationDao.OUTPUT_WAV_FILENAME);
	}

	private static String findMin(Map<String, PathStats> stats) {
		PathStats min = null;
		String minKey = null;
		for (Entry<String, PathStats> cur : stats.entrySet()) {
			if (min == null || cur.getValue().getLastUpdateTime() < min.getLastUpdateTime()) {
				min = cur.getValue();
				minKey = cur.getKey();
			}
		}
		return minKey;
	}
}

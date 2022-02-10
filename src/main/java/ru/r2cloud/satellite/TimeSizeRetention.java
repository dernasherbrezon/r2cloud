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
	private final long maxSize;
	private long allObservationsSize = 0;

	public TimeSizeRetention(long maxSize, Path basedir) {
		this.maxSize = maxSize;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(basedir)) {
			for (Path curSatellite : ds) {
				Path dataRoot = curSatellite.resolve("data");
				if (!Files.exists(dataRoot)) {
					// satellite dir without observations shouldn't exist
					Util.deleteDirectory(curSatellite);
					continue;
				}
				try (DirectoryStream<Path> observationDirs = Files.newDirectoryStream(dataRoot)) {
					for (Path curObservation : observationDirs) {
						indexAndCleanup(curObservation);
					}
				}
			}
		} catch (IOException e) {
			LOG.error("unable to index all", e);
		}
	}

	public void indexAndCleanup(Path curObservation) {
		long minTime = Long.MAX_VALUE;
		long totalSize = 0;
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(curObservation)) {
			for (Path file : dir) {
				FileTime time = Files.getLastModifiedTime(file);
				minTime = Math.min(minTime, time.toMillis());
				totalSize += Files.size(file);
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
			allObservationsSize += newStats.getSize();
			while (allObservationsSize > maxSize) {
				String minKey = findMin();
				if (minKey == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				PathStats min = statsByPath.remove(minKey);
				if (min == null) {
					LOG.info("no observation with minimum time found"); // this is weird because at least one was just added
					break;
				}
				LOG.info("deleting old observation: {} last update time: {}", min.getPath().toString(), new Date(min.getLastUpdateTime()));
				if (Util.deleteDirectory(min.getPath())) {
					allObservationsSize -= min.getSize();
				}
			}
		}

	}

	private String findMin() {
		PathStats min = null;
		String minKey = null;
		for (Entry<String, PathStats> cur : statsByPath.entrySet()) {
			if (min == null || cur.getValue().getLastUpdateTime() < min.getLastUpdateTime()) {
				min = cur.getValue();
				minKey = cur.getKey();
			}
		}
		return minKey;
	}
}

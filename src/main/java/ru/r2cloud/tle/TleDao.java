package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class TleDao {

	private static final Logger LOG = LoggerFactory.getLogger(TleDao.class);

	private final Path cacheFileLocation;
	private final Map<String, Tle> cache = new ConcurrentHashMap<>();
	private long lastUpdateTime;

	public TleDao(Configuration config) {
		cacheFileLocation = config.getPathFromProperty("tle.cacheFileLocation");
		cache.putAll(loadTle(cacheFileLocation));
		if (Files.exists(cacheFileLocation)) {
			try {
				lastUpdateTime = Files.getLastModifiedTime(cacheFileLocation).toMillis();
			} catch (IOException e) {
				lastUpdateTime = 0l;
			}
		} else {
			lastUpdateTime = 0l;
		}
	}

	public Map<String, Tle> loadTle() {
		return cache;
	}

	public void saveTle(Map<String, Tle> tle) {
		cache.putAll(tle);
		lastUpdateTime = System.currentTimeMillis();
		saveTle(cacheFileLocation, tle);
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	private static void saveTle(Path file, Map<String, Tle> tle) {
		// ensure temp and output are on the same filestore
		Path tempOutput = file.getParent().resolve("tle.txt.tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tempOutput)) {
			for (Tle cur : tle.values()) {
				w.append(cur.getRaw()[0]);
				w.newLine();
				w.append(cur.getRaw()[1]);
				w.newLine();
				w.append(cur.getRaw()[2]);
				w.newLine();
			}
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to save tle: " + file.toAbsolutePath(), e);
			return;
		}

		try {
			Files.move(tempOutput, file, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOG.error("unable to move .tmp to dst", e);
		}
	}

	private static Map<String, Tle> loadTle(Path file) {
		Map<String, Tle> result = new HashMap<>();
		if (!Files.exists(file)) {
			return result;
		}
		try (BufferedReader in = Files.newBufferedReader(file)) {
			// only first line matters
			String curLine = null;
			while ((curLine = in.readLine()) != null) {
				String line1 = in.readLine();
				if (line1 == null) {
					break;
				}
				String line2 = in.readLine();
				if (line2 == null) {
					break;
				}
				String noradId = line2.substring(2, 2 + 5).trim();
				result.put(noradId, new Tle(new String[] { curLine.trim(), line1, line2 }));
			}
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load tle from cache: " + file.toAbsolutePath(), e);
		}
		return result;
	}
}

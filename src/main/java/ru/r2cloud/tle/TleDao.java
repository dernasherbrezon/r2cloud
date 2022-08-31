package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

	private final File cacheFileLocation;
	private final Map<String, Tle> cache = new ConcurrentHashMap<>();
	private long lastUpdateTime;

	public TleDao(Configuration config) {
		cacheFileLocation = new File(config.getProperty("tle.cacheFileLocation"));
		cache.putAll(loadTle(cacheFileLocation));
		if (cacheFileLocation.exists()) {
			lastUpdateTime = cacheFileLocation.lastModified();
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

	private static void saveTle(File file, Map<String, Tle> tle) {
		// ensure temp and output are on the same filestore
		Path tempOutput = file.toPath().getParent().resolve("tle.txt.tmp");
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
			Util.logIOException(LOG, "unable to save tle: " + file.getAbsolutePath(), e);
			return;
		}

		try {
			Files.move(tempOutput, file.toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOG.error("unable to move .tmp to dst", e);
		}
	}

	private static Map<String, Tle> loadTle(File file) {
		Map<String, Tle> result = new HashMap<>();
		if (!file.exists()) {
			return result;
		}
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
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
			Util.logIOException(LOG, "unable to load tle from cache: " + file.getAbsolutePath(), e);
		}
		return result;
	}
}

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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.WriterConfig;

import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class TleDao {

	private static final Logger LOG = LoggerFactory.getLogger(TleDao.class);

	private final Path cacheFileLocation;
	private final Map<String, Tle> cache = new ConcurrentHashMap<>();
	private final Map<String, Tle> cacheByName = new ConcurrentHashMap<>();
	private long lastUpdateTime;

	public TleDao(Configuration config) {
		cacheFileLocation = config.getPathFromProperty("tle.cacheFileLocation");
		index(loadTle(cacheFileLocation));
		if (!cache.isEmpty() && Files.exists(cacheFileLocation)) {
			try {
				lastUpdateTime = Files.getLastModifiedTime(cacheFileLocation).toMillis();
			} catch (IOException e) {
				lastUpdateTime = 0l;
			}
		} else {
			lastUpdateTime = 0l;
		}
	}

	public Map<String, Tle> findAll() {
		return cache;
	}

	public Tle find(String id, String name) {
		if (id == null) {
			return null;
		}
		Tle result = cache.get(id);
		if (result != null) {
			return result;
		}
		if (name == null) {
			return null;
		}
		return cacheByName.get(name);
	}

	private void index(Map<String, Tle> tleById) {
		cache.clear();
		cacheByName.clear();
		putAll(tleById);
	}

	public void putAll(Map<String, Tle> tleById) {
		cache.putAll(tleById);
		for (Tle cur : tleById.values()) {
			cacheByName.put(cur.getRaw()[0], cur);
		}
	}

	public void saveTle(Map<String, Tle> tle) {
		index(tle);
		lastUpdateTime = System.currentTimeMillis();
		saveTle(cacheFileLocation, tle);
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	private static void saveTle(Path file, Map<String, Tle> tle) {
		JsonArray output = new JsonArray();
		for (Tle cur : tle.values()) {
			output.add(cur.toJson());
		}
		// ensure temp and output are on the same filestore
		Path tempOutput = file.getParent().resolve("tle.json.tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tempOutput)) {
			output.writeTo(w, WriterConfig.PRETTY_PRINT);
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
		JsonArray output = null;
		try (BufferedReader in = Files.newBufferedReader(file)) {
			output = Json.parse(in).asArray();
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load tle from cache: " + file.toAbsolutePath(), e);
			return result;
		}
		for (int i = 0; i < output.size(); i++) {
			Tle cur = Tle.fromJson(output.get(i).asObject());
			if (cur == null) {
				continue;
			}
			String noradId = cur.getRaw()[cur.getRaw().length - 1].substring(2, 2 + 5).trim();
			result.put(noradId, cur);
		}
		return result;
	}
}

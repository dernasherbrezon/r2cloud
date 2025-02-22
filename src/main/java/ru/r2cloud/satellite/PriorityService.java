package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class PriorityService {

	private static final Logger LOG = LoggerFactory.getLogger(PriorityService.class);

	private final Configuration config;
	private final Path cacheFileLocation;
	private final Map<String, Integer> cache = new ConcurrentHashMap<>();
	private final int timeout;
	private final Clock clock;
	private long lastUpdateTime;

	public PriorityService(Configuration config, Clock clock) {
		this.config = config;
		this.clock = clock;
		this.timeout = config.getInteger("satellites.priority.timeout");
		cacheFileLocation = config.getPathFromProperty("satellites.priority.location");
		if (Files.exists(cacheFileLocation)) {
			List<String> ids = new ArrayList<>();
			try (BufferedReader r = Files.newBufferedReader(cacheFileLocation)) {
				String curLine = null;
				while ((curLine = r.readLine()) != null) {
					curLine = curLine.trim();
					if (curLine.length() == 0) {
						continue;
					}
					ids.add(curLine);
				}
			} catch (Exception e) {
				LOG.info("unable to load priorities from: {}", cacheFileLocation, e);
			}
			index(ids);
			try {
				lastUpdateTime = Files.getLastModifiedTime(cacheFileLocation).toMillis();
			} catch (IOException e) {
				lastUpdateTime = 0l;
			}
		} else {
			lastUpdateTime = 0l;
		}
	}

	private void index(List<String> ids) {
		// store priorities inverted so that default ("0") will be the lowest
		// and the lowest configured will be "1" which is still higher than the default
		for (int i = 0; i < ids.size(); i++) {
			cache.put(ids.get(i), ids.size() - i);
		}
	}

	public void reload() {
		String url = config.getProperty("satellites.priority.url");
		if (url == null) {
			return;
		}
		long periodMillis = config.getLong("satellites.priority.periodMillis");
		long currentTime = clock.millis();
		if (lastUpdateTime != 0 && currentTime - lastUpdateTime < periodMillis) {
			return;
		}
		lastUpdateTime = currentTime;
		List<String> ids = download(url);
		// error happened
		if (ids == null) {
			return;
		}
		// empty list - intentional clean up of all priorities. should be saved to disk
		index(ids);
		save(cacheFileLocation, ids);
	}

	public Integer find(String id) {
		return cache.get(id);
	}

	public Map<String, Integer> findAll() {
		return cache;
	}

	private List<String> download(String url) {
		HttpURLConnection con = null;
		try {
			LOG.info("loading priorities from: {}", url);
			URL obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			con.setRequestProperty("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to get priorities from {}. response code: {}. See logs for details", url, responseCode);
				Util.toLog(LOG, con.getErrorStream());
				return null;
			} else {
				List<String> result = new ArrayList<>();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					// only first line matters
					String curLine = null;
					while ((curLine = in.readLine()) != null) {
						curLine = curLine.trim();
						if (curLine.length() == 0) {
							continue;
						}
						result.add(curLine);
					}
				}
				LOG.info("received priorities {} for satellites", result.size());
				return result;
			}
		} catch (Exception e) {
			Util.logIOException(LOG, "unable to load priorities from " + url, e);
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private static void save(Path file, List<String> priorities) {
		// ensure temp and output are on the same filestore
		Path tempOutput = file.getParent().resolve("priorities.txt.tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tempOutput)) {
			for (String cur : priorities) {
				w.append(cur);
				w.newLine();
			}
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to save: " + file.toAbsolutePath(), e);
			return;
		}

		try {
			Files.move(tempOutput, file, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOG.error("unable to move .tmp to dst", e);
		}
	}

}

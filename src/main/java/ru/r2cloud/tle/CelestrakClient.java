package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class CelestrakClient {

	private static final Logger LOG = LoggerFactory.getLogger(CelestrakClient.class);

	private final List<String> urls;
	private final Clock clock;
	private final int timeout;

	public CelestrakClient(Configuration props, Clock clock) {
		this.clock = clock;
		this.urls = props.getProperties("tle.urls");
		this.timeout = props.getInteger("tle.timeout");
	}

	public Map<String, Tle> downloadTle() {
		Map<String, Tle> result = new HashMap<>();
		for (String cur : urls) {
			result.putAll(downloadTle(cur));
		}
		return result;
	}

	private Map<String, Tle> downloadTle(String location) {
		HttpURLConnection con = null;
		Map<String, Tle> result = new HashMap<>();
		try {
			LOG.info("loading tle from: {}", location);
			URL obj = new URL(location);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			con.setRequestProperty("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to get tle from {}. response code: {}. See logs for details", location, responseCode);
				Util.toLog(LOG, con.getErrorStream());
			} else {
				if (location.contains("json")) {
					result = readJsonFormat(con, obj);
				} else {
					result = readOldFormat(con, obj);
				}
			}
			LOG.info("received tle for {} satellites", result.size());
		} catch (Exception e) {
			Util.logIOException(LOG, "unable to get tle from " + location, e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return result;
	}

	private Map<String, Tle> readJsonFormat(HttpURLConnection con, URL obj) throws IOException {
		Map<String, Tle> result = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			JsonValue all = Json.parse(in);
			if (!all.isArray()) {
				LOG.info("invalid response. expected array");
				return result;
			}
			JsonArray arr = all.asArray();
			for (JsonValue cur : arr) {
				if (!cur.isObject()) {
					continue;
				}
				Tle value = Tle.fromJson(cur.asObject());
				value.setLastUpdateTime(clock.millis());
				value.setSource(obj.getHost());
				result.put(value.getObjectName(), value);
			}
		}
		return result;
	}

	private Map<String, Tle> readOldFormat(HttpURLConnection con, URL obj) throws IOException {
		Map<String, Tle> result = new HashMap<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
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
				JsonObject json = new JsonObject();
				json.add("line1", curLine.trim());
				json.add("line2", line1);
				json.add("line3", line2);
				Tle value = Tle.fromJson(json);
				value.setLastUpdateTime(clock.millis());
				value.setSource(obj.getHost());
				result.put(noradId, value);
			}
		}
		return result;
	}

}

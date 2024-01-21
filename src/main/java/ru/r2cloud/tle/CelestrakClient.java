package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class CelestrakClient {

	private static final Logger LOG = LoggerFactory.getLogger(CelestrakClient.class);

	private final List<String> urls;
	private final int timeout;

	public CelestrakClient(Configuration props) {
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
						Tle value = new Tle(new String[] { curLine.trim(), line1, line2 });
						value.setLastUpdateTime(System.currentTimeMillis());
						value.setSource(obj.getHost());
						result.put(noradId, value);
					}
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

}

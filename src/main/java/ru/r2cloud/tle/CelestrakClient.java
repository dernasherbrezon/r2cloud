package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Util;

public class CelestrakClient {

	private static final int TIMEOUT = 60 * 1000;
	private static final Logger LOG = LoggerFactory.getLogger(CelestrakClient.class);
	private final String host;
	private final String calpolyHost;

	public CelestrakClient(String host) {
		this(host, null);
	}

	public CelestrakClient(String host, String calpolyHost) {
		this.host = host;
		this.calpolyHost = calpolyHost;
	}

	public Map<String, Tle> getTleForActiveSatellites() {
		Map<String, Tle> result = new HashMap<>();
		if (calpolyHost != null) {
			result.putAll(loadTle(calpolyHost + "/~ops/keps/kepler.txt"));
		}
		result.putAll(loadTle(host + "/NORAD/elements/satnogs.txt"));
		result.putAll(loadTle(host + "/NORAD/elements/active.txt"));
		return result;
	}

	private static Map<String, Tle> loadTle(String location) {
		HttpURLConnection con = null;
		Map<String, Tle> result = new HashMap<>();
		try {
			URL obj = new URL(location);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(TIMEOUT);
			con.setReadTimeout(TIMEOUT);
			con.setRequestProperty("User-Agent", R2Cloud.getVersion() + " info@r2cloud.ru");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to get weather tle. response code: {}. See logs for details", responseCode);
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
						if (curLine.equalsIgnoreCase("ACRUX 1")) {
							System.out.println("here");
						}
						result.put(curLine.trim(), new Tle(new String[] { curLine.trim(), line1, line2 }));
					}
				}
			}
		} catch (Exception e) {
			Util.logIOException(LOG, "unable to get tle", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return result;
	}

}

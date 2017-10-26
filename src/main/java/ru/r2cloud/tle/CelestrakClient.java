package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.TLE;
import ru.r2cloud.util.Util;

public class CelestrakClient {

	private static final Logger LOG = LoggerFactory.getLogger(CelestrakClient.class);
	private final String host;

	public CelestrakClient(String host) {
		this.host = host;
	}

	public Map<String, TLE> getWeatherTLE() {
		HttpURLConnection con = null;
		Map<String, TLE> result = new HashMap<String, TLE>();
		try {
			URL obj = new URL(host + "/NORAD/elements/weather.txt");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to get weather tle. response code: " + responseCode + ". See logs for details");
				Util.toLog(LOG, con.getInputStream());
			} else {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					// only first line matters
					String curLine = null;
					while ((curLine = in.readLine()) != null) {
						// Satellite satellite =
						// satelliteDao.findByName(curLine.trim());
						String line1 = in.readLine();
						if (line1 == null) {
							break;
						}
						String line2 = in.readLine();
						if (line2 == null) {
							break;
						}
						result.put(curLine.trim(), new TLE(new String[] { curLine.trim(), line1, line2 }));
					}
				}
			}
		} catch (Exception e) {
			LOG.error("unable to get weather tle", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return result;
	}

}

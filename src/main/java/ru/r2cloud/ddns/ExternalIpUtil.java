package ru.r2cloud.ddns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Util;

final class ExternalIpUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ExternalIpUtil.class);

	public static String getExternalIp() {
		HttpURLConnection con = null;
		try {
			URL obj = new URL("http://checkip.amazonaws.com");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			int responseCode = con.getResponseCode();
			String result = null;
			if (responseCode != 200) {
				LOG.error("unable to get external ip. response code: {}. See logs for details", responseCode);
				Util.toLog(LOG, con.getInputStream());
			} else {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					// only first line matters
					result = in.readLine();
				}
			}

			return result;
		} catch (Exception e) {
			LOG.error("unable to get an external ip", e);
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private ExternalIpUtil() {
		//do nothing
	}
}

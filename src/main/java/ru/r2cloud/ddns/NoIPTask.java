package ru.r2cloud.ddns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.r2cloud.uitl.Configuration;

public class NoIPTask implements Runnable {

	private static final Logger LOG = Logger.getLogger(NoIPTask.class.getName());

	private final String username;
	private final String password;
	private final String domainName;

	public NoIPTask(Configuration config) throws Exception {
		username = getAndValidate(config, "ddns.noip.username");
		password = getAndValidate(config, "ddns.noip.password");
		domainName = getAndValidate(config, "ddns.noip.domain");
	}

	@Override
	public void run() {
		try {
			URL obj = new URL("https://dynupdate.no-ip.com/nic/update?hostname=" + domainName);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.US_ASCII)));
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.log(Level.SEVERE, "unable to update ddns. response code: " + responseCode + ". See logs for details");
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					LOG.info(in.readLine());
				}
			}
			con.disconnect();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "unable to update ddns", e);
		}
	}

	private static String getAndValidate(Configuration config, String name) throws Exception {
		String username = config.getProperty(name);
		if (username == null || username.trim().length() == 0) {
			throw new Exception(name + " cannot be empty");
		}
		return username;
	}

}

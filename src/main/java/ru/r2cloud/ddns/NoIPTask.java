package ru.r2cloud.ddns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.noip.NoIpClient;
import ru.r2cloud.ddns.noip.NoIpException;
import ru.r2cloud.ddns.noip.RetryException;
import ru.r2cloud.util.Configuration;

public class NoIPTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(NoIPTask.class);

	private final Configuration config;
	private final NoIpClient client;
	private final String username;
	private final String password;
	private final String domainName;

	private String currentExternalIp;
	private boolean fatalError = false;
	private Long retryAfter = null;

	public NoIPTask(Configuration config) throws Exception {
		this.config = config;
		username = getAndValidate(config, "ddns.noip.username");
		password = getAndValidate(config, "ddns.noip.password");
		domainName = getAndValidate(config, "ddns.noip.domain");
		client = new NoIpClient("https://dynupdate.no-ip.com", username, password);
		currentExternalIp = config.getProperty("ddns.ip");
		retryAfter = config.getLong("ddns.retry.after.millis");
	}

	@Override
	public void run() {
		if (fatalError) {
			return;
		}
		// skip update due to ddns server error.
		// see the protocol at https://www.noip.com/integrate/response
		if (retryAfter != null) {
			if (retryAfter > System.currentTimeMillis()) {
				return;
			} else {
				config.remove("ddns.retry.after.millis");
				config.update();
				retryAfter = null;
			}
		}

		String externalIp = ExternalIpUtil.getExternalIp();
		if (currentExternalIp != null && currentExternalIp.equals(externalIp)) {
			return;
		}
		LOG.info("ip has changed. old: {} new: {}", currentExternalIp, externalIp);
		try {
			currentExternalIp = client.update(domainName);
			LOG.info("ddns ip updated: {}", currentExternalIp);
			config.setProperty("ddns.ip", currentExternalIp);
			config.update();
		} catch (NoIpException e) {
			LOG.error("unable to update ddns record", e);
			fatalError = true;
		} catch (RetryException e) {
			LOG.info("no-ip failure. retry after: " + e.getRetryTimeout());
			retryAfter = System.currentTimeMillis() + e.getRetryTimeout();
			// save it to show in UI
			config.setProperty("ddns.retry.after.millis", retryAfter);
			config.update();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	private static String getAndValidate(Configuration config, String name) throws Exception {
		String username = config.getProperty(name);
		if (username == null || username.trim().length() == 0) {
			throw new IllegalArgumentException(name + " cannot be empty");
		}
		return username;
	}

}

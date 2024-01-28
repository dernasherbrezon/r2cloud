package ru.r2cloud.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedURL {

	private static final Logger LOG = LoggerFactory.getLogger(SignedURL.class);
	private static final String PASSWORD_PROPERTY_NAME = "server.password";

	private final Configuration config;
	private final Clock clock;

	public SignedURL(Configuration config, Clock clock) {
		this.config = config;
		this.clock = clock;
	}

	public String sign(String path) {
		if (path == null) {
			return null;
		}
		long timestamp = clock.millis();
		String concatChar;
		if (path.contains("?")) {
			concatChar = "&";
		} else {
			concatChar = "?";
		}
		return path + concatChar + "hash=" + URLEncoder.encode(computeMD5(path + timestamp + config.getProperty(PASSWORD_PROPERTY_NAME)), StandardCharsets.UTF_8) + "&timestamp=" + timestamp;
	}

	public boolean validate(String path, Map<String, List<String>> parameters) {
		if (path == null || parameters == null || parameters.isEmpty()) {
			return false;
		}
		String hash = getFirstParameter(parameters, "hash");
		String timestampStr = getFirstParameter(parameters, "timestamp");
		if (hash == null || timestampStr == null) {
			LOG.debug("missing hash or timestamp");
			return false;
		}
		String expectedHash = computeMD5(path + timestampStr + config.getProperty(PASSWORD_PROPERTY_NAME));
		if (!expectedHash.equals(hash)) {
			LOG.debug("hash mismatched: {}", hash);
			return false;
		}
		long timestamp;
		try {
			timestamp = Long.valueOf(timestampStr);
		} catch (Exception e) {
			return false;
		}
		if (timestamp + config.getLong("server.static.signed.validMillis") < clock.millis()) {
			LOG.debug("link expired: {}", timestamp);
			return false;
		}
		return true;
	}

	private static String computeMD5(String input) {
		if (input == null) {
			return null;
		}
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(input.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return Hex.encode(digest.digest());
	}

	private static String getFirstParameter(Map<String, List<String>> parameters, String name) {
		List<String> all = parameters.get(name);
		if (all == null || all.isEmpty()) {
			return null;
		}
		return all.get(0);
	}
}

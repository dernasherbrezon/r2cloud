package ru.r2cloud.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Hex;

public class Authenticator {

	private static final String PASSWORD_PROPERTY_NAME = "server.password";
	private static final String SALT_PROPERTY_NAME = "server.salt";
	private static final String LOGIN_PROPERTY_NAME = "server.login";
	private static final Logger LOG = LoggerFactory.getLogger(Authenticator.class);

	private final SecureRandom random = new SecureRandom();

	private final Configuration props;

	private String authenticatedToken;
	private long authenticatedAt;

	private String login;
	private String password;
	private String salt;
	private long maxAgeMillis;

	public Authenticator(Configuration props) {
		this.props = props;
		this.login = normalizeUsername(props.getProperty(LOGIN_PROPERTY_NAME));
		this.password = props.getProperty(PASSWORD_PROPERTY_NAME);
		this.salt = props.getProperty(SALT_PROPERTY_NAME);
		this.maxAgeMillis = props.getLong("server.session.timeout.millis");
	}

	public boolean isAuthenticated(IHTTPSession session) {
		if (authenticatedToken == null) {
			return false;
		}
		if (System.currentTimeMillis() - authenticatedAt > maxAgeMillis) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("session expired");
			}
			authenticatedToken = null;
			return false;
		}
		String values = session.getHeaders().get("authorization");
		if (values == null) {
			return false;
		}
		int index = values.indexOf(' ');
		if (index == -1) {
			return false;
		}
		return values.substring(index + 1).equals(authenticatedToken);
	}

	public boolean isFirstStart() {
		return login == null || login.trim().length() == 0;
	}

	public String authenticate(String login, String password) {
		if (login == null || password == null) {
			return null;
		}
		login = normalizeUsername(login);
		if (this.login == null || !this.login.equals(login)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("login mismatched: {}", login);
			}
			return null;
		}

		String salted = salt(password, salt);
		String passwordToCheck = getPasswordToCheck(salted);
		if (passwordToCheck == null || !passwordToCheck.equals(this.password)) {
			return null;
		}

		byte[] token = new byte[12];
		random.nextBytes(token);

		this.authenticatedToken = Hex.encode(token);
		this.authenticatedAt = System.currentTimeMillis();
		return authenticatedToken;
	}

	private static String getPasswordToCheck(String salted) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LOG.error("sha-256 not found", e);
			return null;
		}

		byte[] digested = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
		return Hex.encode(digested);
	}

	private static String salt(String password, String salt) {
		return password + "{" + salt + "}";
	}

	public void setPassword(String login, String password) {
		if (this.login != null) {
			return;
		}
		this.login = normalizeUsername(login);
		this.salt = UUID.randomUUID().toString();
		this.password = getPasswordToCheck(salt(password, salt));

		reloadProps();
	}

	private void reloadProps() {
		if (login != null) {
			props.setProperty(LOGIN_PROPERTY_NAME, this.login);
		} else {
			props.remove(LOGIN_PROPERTY_NAME);
		}
		if (salt != null) {
			props.setProperty(SALT_PROPERTY_NAME, this.salt);
		} else {
			props.remove(SALT_PROPERTY_NAME);
		}
		if (password != null) {
			props.setProperty(PASSWORD_PROPERTY_NAME, this.password);
		} else {
			props.remove(PASSWORD_PROPERTY_NAME);
		}
		props.update();
	}

	public long getMaxAgeMillis() {
		return maxAgeMillis;
	}

	public void resetPassword(String username) {
		LOG.info("reset password for: {}", username);
		if (username == null || username.trim().length() == 0) {
			return;
		}
		username = normalizeUsername(username);
		if (!username.equals(login)) {
			return;
		}

		this.salt = null;
		this.password = null;
		this.login = null;
		this.authenticatedToken = null;
		this.authenticatedAt = 0L;

		reloadProps();
	}

	private static String normalizeUsername(String username) {
		if (username == null) {
			return null;
		}
		return username.trim().toLowerCase(Locale.UK);
	}

}

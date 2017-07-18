package ru.r2cloud.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.uitl.Hex;

import com.google.common.base.Splitter;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Authenticator {

	private static final Logger LOG = Logger.getLogger(Authenticator.class.getName());

	private final SecureRandom random = new SecureRandom();

	private final Configuration props;
	private final Splitter semicolonSplitter = Splitter.on(';').trimResults().omitEmptyStrings();
	private final Splitter equalsSplitter = Splitter.on('=').trimResults().omitEmptyStrings();

	private String authenticatedJSessionId;
	private long authenticatedAt;

	private String login;
	private String password;
	private String salt;
	private long maxAgeMillis;

	public Authenticator(Configuration props) {
		this.props = props;
		this.login = props.getProperty("server.login");
		this.password = props.getProperty("server.password");
		this.salt = props.getProperty("server.salt");
		this.maxAgeMillis = Long.valueOf(props.getProperty("server.session.timeout.millis"));
	}

	public boolean isAuthenticated(IHTTPSession session) {
		if (authenticatedJSessionId == null) {
			return false;
		}
		if (System.currentTimeMillis() - authenticatedAt > maxAgeMillis) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE, "session expired");
			}
			authenticatedJSessionId = null;
			return false;
		}
		String values = session.getHeaders().get("cookie");
		if (values == null) {
			return false;
		}
		Iterator<String> it = semicolonSplitter.split(values).iterator();
		while (it.hasNext()) {
			Iterator<String> cookie = equalsSplitter.split(it.next()).iterator();
			if (cookie.hasNext()) {
				String name = cookie.next();
				if (!name.equals("JSESSIONID")) {
					continue;
				}
				if (!cookie.hasNext()) {
					continue;
				}
				String value = cookie.next();
				if (value.equals(authenticatedJSessionId)) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	public boolean isAuthenticationRequired(IHTTPSession session) {
		if (session.getUri().startsWith("/admin/")) {
			return true;
		}
		return false;
	}

	public String authenticate(String login, String password) {
		if (login == null || password == null) {
			return null;
		}
		if (!this.login.equals(login)) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE, "login mismatched: " + login);
			}
			return null;
		}

		String salted = salt(password, salt);
		String passwordToCheck = getPasswordToCheck(salted);
		if (!passwordToCheck.equals(this.password)) {
			return null;
		}

		byte[] cookie = new byte[12];
		random.nextBytes(cookie);

		this.authenticatedJSessionId = new String(Hex.encode(cookie));
		this.authenticatedAt = System.currentTimeMillis();
		return authenticatedJSessionId;
	}

	private static String getPasswordToCheck(String salted) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LOG.log(Level.SEVERE, "sha-256 not found", e);
			return null;
		}

		byte[] digested = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
		String passwordToCheck = new String(Hex.encode(digested));
		return passwordToCheck;
	}

	private static String salt(String password, String salt) {
		return password + "{" + salt + "}";
	}

	public void setPassword(String login, String password) {
		this.login = login;
		this.salt = UUID.randomUUID().toString();
		this.password = getPasswordToCheck(salt(password, salt));

		props.put("server.login", this.login);
		props.put("server.salt", this.salt);
		props.put("server.password", this.password);
		props.update();
	}
	
	public long getMaxAgeMillis() {
		return maxAgeMillis;
	}

}

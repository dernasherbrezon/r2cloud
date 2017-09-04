package ru.r2cloud.uitl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class Configuration {

	private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

	private final Properties userSettings = new Properties();
	private final String userSettingsLocation;
	private static Set<PosixFilePermission> MODE600 = new HashSet<PosixFilePermission>();
	
	private final Properties systemSettings = new Properties();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

	public Configuration(String propertiesLocation) {
		try (InputStream is = new FileInputStream(propertiesLocation)) {
			systemSettings.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
		userSettingsLocation = System.getProperty("user.home") + File.separator + ".r2cloud";
		if (new File(userSettingsLocation).exists()) {
			try (InputStream is = new FileInputStream(userSettingsLocation)) {
				userSettings.load(is);
			} catch (Exception e) {
				throw new RuntimeException("Unable to load user properties", e);
			}
		}
	}

	public Object setProperty(Object key, Object value) {
		return userSettings.put(key, value);
	}

	public void update() {
		try (FileWriter fos = new FileWriter(userSettingsLocation)) {
			userSettings.store(fos, "updated");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			Files.setPosixFilePermissions(Paths.get(userSettingsLocation), MODE600);
		} catch (IOException e) {
			LOG.info("unable to setup 600 permissions: " + e.getMessage());
		}
	}

	public Long getLong(String name) {
		String strValue = getProperty(name);
		if (strValue == null) {
			return null;
		}
		return Long.valueOf(strValue);
	}

	public Integer getInteger(String name) {
		String strValue = getProperty(name);
		if (strValue == null) {
			return null;
		}
		return Integer.valueOf(strValue);
	}

	public String getProperty(String name) {
		String result = systemSettings.getProperty(name);
		if (result != null) {
			return result;
		}
		return userSettings.getProperty(name);
	}
	
	public void remove(String name) {
		userSettings.remove(name);
	}

}

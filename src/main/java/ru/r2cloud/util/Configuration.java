package ru.r2cloud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSType;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

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

	public boolean getBoolean(String string) {
		String str = getProperty(string);
		if (str == null) {
			return false;
		}
		return Boolean.valueOf(str);
	}

	public Double getDouble(String name) {
		String str = getProperty(name);
		if (str == null) {
			return null;
		}
		return Double.valueOf(str);
	}

	public String getProperty(String name) {
		String result = userSettings.getProperty(name);
		if (result != null) {
			return result;
		}
		return systemSettings.getProperty(name);
	}

	public DDNSType getDdnsType(String name) {
		String str = getProperty(name);
		if (str == null || str.trim().length() == 0) {
			return null;
		}
		return DDNSType.valueOf(str);
	}

	public void remove(String name) {
		userSettings.remove(name);
	}

	public List<String> getList(String name) {
		String str = getProperty(name);
		if (str == null) {
			return Collections.emptyList();
		}
		return Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(str));
	}
}

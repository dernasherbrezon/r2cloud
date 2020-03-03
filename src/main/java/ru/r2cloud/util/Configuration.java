package ru.r2cloud.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.PpmType;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private final Properties userSettings = new Properties();
	private final Path userSettingsLocation;
	private final FileSystem fs;
	private static final Set<PosixFilePermission> MODE600 = new HashSet<PosixFilePermission>();

	private final Properties systemSettings = new Properties();
	private final Map<String, List<ConfigListener>> listeners = new ConcurrentHashMap<String, List<ConfigListener>>();
	private final Set<String> changedProperties = new HashSet<String>();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

	public Configuration(InputStream systemSettingsLocation, String userSettingsLocation, FileSystem fs) throws IOException {
		systemSettings.load(systemSettingsLocation);
		this.userSettingsLocation = fs.getPath(userSettingsLocation);
		this.fs = fs;
		loadUserSettings();
	}

	private void loadUserSettings() throws IOException {
		if (Files.exists(userSettingsLocation)) {
			try (InputStream is = Files.newInputStream(userSettingsLocation)) {
				userSettings.load(is);
			}
		}
	}

	public String setProperty(String key, Long value) {
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, Integer value) {
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, boolean value) {
		return setProperty(key, String.valueOf(value));
	}

	public Path getSatellitesBasePath() {
		return fs.getPath(getProperty("satellites.basepath.location"));
	}

	public Path getPathFromProperty(String propertyName) {
		return fs.getPath(getProperty(propertyName));
	}

	public String setProperty(String key, String value) {
		synchronized (changedProperties) {
			changedProperties.add(key);
		}
		return (String) userSettings.put(key, value);
	}

	public void update() {
		Path tempPath = userSettingsLocation.getParent().resolve("user.properties.tmp");
		try (BufferedWriter fos = Files.newBufferedWriter(tempPath)) {
			userSettings.store(fos, "updated");
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		try {
			Files.setPosixFilePermissions(tempPath, MODE600);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		try {
			// temp and dest are on the same filestore
			// AtomicMoveNotSupportedException shouldn't happen
			Files.move(tempPath, userSettingsLocation, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		Set<ConfigListener> toNotify = new HashSet<ConfigListener>();
		synchronized (changedProperties) {
			for (String cur : changedProperties) {
				List<ConfigListener> curListener = listeners.get(cur);
				if (curListener == null) {
					continue;
				}
				toNotify.addAll(curListener);
			}
			changedProperties.clear();
		}

		for (ConfigListener cur : toNotify) {
			try {
				cur.onConfigUpdated();
			} catch (Exception e) {
				LOG.error("unable to notify listener: " + cur, e);
			}
		}
	}

	public long getThreadPoolShutdownMillis() {
		return getLong("threadpool.shutdown.millis");
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

	public PpmType getPpmType() {
		String str = getProperty("ppm.calculate.type");
		if (str == null) {
			return PpmType.AUTO;
		}
		try {
			return PpmType.valueOf(str);
		} catch (Exception e) {
			LOG.error("invalid ppm type: " + str + " default to: AUTO", e);
			return PpmType.AUTO;
		}
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
		if (result != null && result.trim().length() != 0) {
			return result;
		}
		result = systemSettings.getProperty(name);
		if (result == null || result.trim().length() == 0) {
			return null;
		}
		return result;
	}

	public DDNSType getDdnsType(String name) {
		String str = getProperty(name);
		if (str == null || str.trim().length() == 0) {
			return null;
		}
		return DDNSType.valueOf(str);
	}

	public void remove(String name) {
		synchronized (changedProperties) {
			changedProperties.add(name);
		}
		userSettings.remove(name);
	}

	public void subscribe(ConfigListener listener, String... names) {
		for (String cur : names) {
			List<ConfigListener> previous = this.listeners.get(cur);
			if (previous == null) {
				previous = new ArrayList<>();
				this.listeners.put(cur, previous);
			}
			previous.add(listener);
		}
	}

	public File getTempDirectory() {
		String tmpDirectory = getProperty("server.tmp.directory");
		if (tmpDirectory != null) {
			return new File(tmpDirectory);
		}
		return new File(System.getProperty("java.io.tmpdir"));
	}

	public Path getTempDirectoryPath() {
		String tmpDirectory = getProperty("server.tmp.directory");
		if (tmpDirectory != null) {
			return fs.getPath(tmpDirectory);
		}
		return fs.getPath(System.getProperty("java.io.tmpdir"));
	}

	public Path getPath(String filename) {
		return fs.getPath(filename);
	}
}

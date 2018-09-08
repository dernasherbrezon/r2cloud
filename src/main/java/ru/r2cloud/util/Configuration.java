package ru.r2cloud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSType;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private final Properties userSettings = new Properties();
	private final String userSettingsLocation;
	private static Set<PosixFilePermission> MODE600 = new HashSet<PosixFilePermission>();

	private final Properties systemSettings = new Properties();
	private final Map<String, List<ConfigListener>> listeners = new ConcurrentHashMap<String, List<ConfigListener>>();
	private final Set<String> changedProperties = new HashSet<String>();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

	public Configuration(InputStream systemSettingsLocation, String userSettingsLocation) throws IOException {
		systemSettings.load(systemSettingsLocation);
		this.userSettingsLocation = userSettingsLocation;
		loadUserSettings(userSettingsLocation);
	}

	public Configuration(String systemSettingsLocation, String userSettingsLocation) {
		try (InputStream is = new FileInputStream(systemSettingsLocation)) {
			systemSettings.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
		this.userSettingsLocation = userSettingsLocation;
		loadUserSettings(userSettingsLocation);
	}

	private void loadUserSettings(String userSettingsLocation) {
		if (new File(userSettingsLocation).exists()) {
			try (InputStream is = new FileInputStream(userSettingsLocation)) {
				userSettings.load(is);
			} catch (Exception e) {
				throw new RuntimeException("Unable to load user properties", e);
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

	public String setProperty(String key, String value) {
		synchronized (changedProperties) {
			changedProperties.add(key);
		}
		return (String) userSettings.put(key, value);
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

	public List<String> getOptions(String name, Options supportedOptions) {
		String args = getProperty(name);
		if (args == null) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		String[] params = args.split(" ");
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(supportedOptions, params);
			if (!line.getArgList().isEmpty()) {
				StringBuilder notFound = new StringBuilder();
				notFound.append("Unsupported args: ");
				for (Object cur : line.getArgList()) {
					notFound.append(cur).append(" ");
				}
				throw new RuntimeException(notFound.toString());
			}
			for (Option cur : line.getOptions()) {
				if (cur.getLongOpt() != null) {
					result.add(cur.getLongOpt());
				} else {
					continue;
				}
				if (cur.getValue() != null) {
					result.add(cur.getValue());
				}
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return result;
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

	public List<String> getList(String name) {
		String str = getProperty(name);
		if (str == null) {
			return Collections.emptyList();
		}
		return Util.splitComma(str);
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

}

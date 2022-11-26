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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;
import ru.r2cloud.model.SdrType;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	public static final String LORA_AT_DEVICE_PREFIX = "lora-at-";

	private final Properties userSettings = new Properties();
	private final Path userSettingsLocation;
	private final FileSystem fs;
	private static final Set<PosixFilePermission> MODE600 = new HashSet<>();

	private final Properties systemSettings = new Properties();
	private final Map<String, List<ConfigListener>> listeners = new ConcurrentHashMap<>();
	private final Set<String> changedProperties = new HashSet<>();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

	public Configuration(InputStream systemSettingsLocation, String userSettingsLocation, String commonSettingsLocation, FileSystem fs) throws IOException {
		systemSettings.load(systemSettingsLocation);
		try (InputStream is = Configuration.class.getClassLoader().getResourceAsStream(commonSettingsLocation)) {
			Properties commonProps = new Properties();
			commonProps.load(is);
			systemSettings.putAll(commonProps);
		}
		this.userSettingsLocation = fs.getPath(userSettingsLocation);
		this.fs = fs;
		loadUserSettings();
	}

	public Configuration(InputStream systemSettingsLocation, String userSettingsLocation, FileSystem fs) throws IOException {
		this(systemSettingsLocation, userSettingsLocation, "config-common.properties", fs);
	}

	private void loadUserSettings() throws IOException {
		if (Files.exists(userSettingsLocation)) {
			try (InputStream is = Files.newInputStream(userSettingsLocation)) {
				userSettings.load(is);
			}
		}
	}

	public String setProperty(String key, Double value) {
		return setProperty(key, String.valueOf(value));
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
		Set<ConfigListener> toNotify = new HashSet<>();
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
				LOG.error("unable to notify listener: {}", cur, e);
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
		return Long.valueOf(strValue.trim());
	}

	public Integer getInteger(String name) {
		String strValue = getProperty(name);
		if (strValue == null) {
			return null;
		}
		return Integer.valueOf(strValue);
	}

	public List<Integer> getIntegerList(String name) {
		List<String> props = getProperties(name);
		List<Integer> result = new ArrayList<>(props.size());
		for (String cur : props) {
			try {
				result.add(Integer.parseInt(cur.trim()));
			} catch (NumberFormatException e) {
				LOG.error("unable to parse: {}", cur, e);
			}
		}
		return result;
	}

	public byte[] getByteArray(String name) {
		List<String> props = getProperties(name);
		byte[] result = new byte[props.size()];
		for (int i = 0; i < props.size(); i++) {
			result[i] = Byte.parseByte(props.get(i));
		}
		return result;
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
			if (result.trim().length() == 0) {
				return null;
			}
			return result;
		}
		result = systemSettings.getProperty(name);
		if (result == null || result.trim().length() == 0) {
			return null;
		}
		return result;
	}

	public SdrType getSdrType() {
		return SdrType.valueOf(getProperty("satellites.sdr").toUpperCase(Locale.UK));
	}

	public List<DeviceConfiguration> getSdrConfigurations() {
		List<String> sdrDevices = getProperties("sdr.devices");
		if (sdrDevices.isEmpty()) {
			return Collections.emptyList();
		}
		List<DeviceConfiguration> result = new ArrayList<>(sdrDevices.size());
		for (String cur : sdrDevices) {
			DeviceConfiguration config = new DeviceConfiguration();
			String prefix = "sdr.device." + cur + ".";
			config.setMinimumFrequency(getLong(prefix + "minFrequency"));
			config.setMaximumFrequency(getLong(prefix + "maxFrequency"));
			config.setRtlDeviceId(getInteger(prefix + "rtlsdr.index"));
			Integer oldDeviceIdProperty = getInteger("satellites.rtlsdr.device.index");
			if (oldDeviceIdProperty != null) {
				config.setRtlDeviceId(oldDeviceIdProperty);
			}
			config.setGain(getDouble(prefix + "rtlsdr.gain").floatValue());
			Double oldGain = getDouble("satellites.rtlsdr.gain");
			if (oldGain != null) {
				config.setGain(oldGain.floatValue());
			}
			config.setBiast(getBoolean(prefix + "rtlsdr.biast"));
			String oldBiast = getProperty("satellites.rtlsdr.biast");
			if (oldBiast != null) {
				config.setBiast(Boolean.valueOf(oldBiast));
			}
			config.setPpm(getInteger(prefix + "ppm"));
			Integer oldPpm = getInteger("ppm.current");
			if (oldPpm != null) {
				config.setPpm(oldPpm);
			}
			config.setId("sdr-" + config.getRtlDeviceId());
			boolean rotatorEnabled = getBoolean(prefix + "rotator.enabled");
			String oldRotatorConfig = getProperty("rotator.enabled");
			if (oldRotatorConfig != null) {
				rotatorEnabled = Boolean.valueOf(oldRotatorConfig);
			}
			if (rotatorEnabled) {
				config.setRotatorConfiguration(getRotatorConfiguration(prefix));
			}
			config.setSdrServerConfiguration(getSdrServerConfiguration(prefix));
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraConfigurations() {
		List<String> loraDevices = getProperties("r2lora.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("r2lora.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			Integer gain = getInteger("r2lora.device." + cur + ".gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			DeviceConfiguration config = new DeviceConfiguration();
			config.setHostport(getProperty("r2lora.device." + cur + ".hostport"));
			config.setUsername(getProperty("r2lora.device." + cur + ".username"));
			config.setPassword(getProperty("r2lora.device." + cur + ".password"));
			config.setTimeout(timeout);
			config.setId("lora-" + config.getHostport());
			config.setRotatorConfiguration(getRotatorConfiguration("r2lora.device." + cur + "."));
			config.setGain(gain);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtConfigurations() {
		List<String> loraDevices = getProperties("loraat.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraat.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			Integer gain = getInteger("loraat.device." + cur + ".gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			DeviceConfiguration config = new DeviceConfiguration();
			config.setHostport(getProperty("loraat.device." + cur + ".port"));
			config.setTimeout(timeout);
			config.setId("loraat-" + config.getHostport());
			config.setRotatorConfiguration(getRotatorConfiguration("loraat.device." + cur + "."));
			config.setGain(gain);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtBluetoothConfigurations() {
		List<String> loraDevices = getProperties("loraatble.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraatble.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			Integer gain = getInteger("loraatble.device." + cur + ".gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			DeviceConfiguration config = new DeviceConfiguration();
			config.setHostport(getProperty("loraatble.device." + cur + ".hostport"));
			config.setBluetoothAddress(getProperty("loraatble.device." + cur + ".btaddress"));
			config.setTimeout(timeout);
			config.setId(LORA_AT_DEVICE_PREFIX + config.getBluetoothAddress());
			config.setRotatorConfiguration(getRotatorConfiguration("loraatble.device." + cur + "."));
			config.setGain(gain);
			result.add(config);
		}
		return result;
	}

	private SdrServerConfiguration getSdrServerConfiguration(String prefix) {
		String hostname = getProperty(prefix + "sdrserver.host");
		if (hostname == null) {
			return null;
		}
		Integer port = getInteger(prefix + "sdrserver.port");
		if (port == null) {
			return null;
		}
		SdrServerConfiguration result = new SdrServerConfiguration();
		result.setHost(hostname);
		result.setPort(port);
		result.setBasepath(getProperty(prefix + "sdrserver.basepath"));
		result.setTimeout(getInteger(prefix + "sdrserver.timeout"));
		result.setUseGzip(getBoolean(prefix + "sdrserver.usegzip"));

		String oldHostname = getProperty("satellites.sdrserver.host");
		if (oldHostname != null) {
			result.setHost(oldHostname);
		}
		Integer oldPort = getInteger("satellites.sdrserver.port");
		if (oldPort != null) {
			result.setPort(oldPort);
		}
		String oldBasePath = getProperty("satellites.sdrserver.basepath");
		if (oldBasePath != null) {
			result.setBasepath(oldBasePath);
		}
		Integer oldTimeout = getInteger("satellites.sdrserver.timeout");
		if (oldTimeout != null) {
			result.setTimeout(oldTimeout);
		}
		String oldGzip = getProperty("satellites.sdrserver.usegzip");
		if (oldGzip != null) {
			result.setUseGzip(Boolean.valueOf(oldGzip));
		}
		return result;
	}

	private RotatorConfiguration getRotatorConfiguration(String prefix) {
		String hostname = getProperty(prefix + "rotctrld.hostname");
		if (hostname == null) {
			return null;
		}
		Integer port = getInteger(prefix + "rotctrld.port");
		if (port == null) {
			return null;
		}
		Integer timeout = getInteger(prefix + "rotctrld.timeout");
		if (timeout == null) {
			// old property
			timeout = getInteger("rotator.rotctrld.timeout");
		}
		if (timeout == null) {
			return null;
		}
		Double tolerance = getDouble(prefix + "rotator.tolerance");
		if (tolerance == null) {
			// old property
			tolerance = getDouble("rotator.tolerance");
		}
		if (tolerance == null) {
			return null;
		}
		Integer cycleMillis = getInteger(prefix + "rotator.cycleMillis");
		if (cycleMillis == null) {
			// old property
			cycleMillis = getInteger("rotator.cycleMillis");
		}
		if (cycleMillis == null) {
			return null;
		}
		RotatorConfiguration result = new RotatorConfiguration();
		result.setId("rotator-" + hostname + ":" + port);
		result.setHostname(hostname);
		result.setPort(port);
		result.setTimeout(timeout);
		result.setTolerance(getThreadPoolShutdownMillis());
		result.setTolerance(tolerance);
		result.setCycleMillis(cycleMillis);
		return result;
	}

	public List<String> getProperties(String name) {
		String rawValue = getProperty(name);
		if (rawValue == null) {
			return Collections.emptyList();
		}
		return Util.splitComma(rawValue);
	}

	public void setList(String name, List<String> urls) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < urls.size(); i++) {
			if (i != 0) {
				str.append(",");
			}
			str.append(urls.get(i));
		}
		setProperty(name, str.toString());
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

	public DemodulatorType getDemodulatorType(Modulation modulation) {
		String demodulatorType = getProperty("satellites.demod." + modulation);
		if (demodulatorType == null) {
			return DemodulatorType.JRADIO;
		}
		return DemodulatorType.valueOf(demodulatorType);
	}

}

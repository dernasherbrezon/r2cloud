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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.hipparchus.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	public static final String LORA_AT_DEVICE_PREFIX = "loraat-";

	private final Properties userSettings;
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
		userSettings = new Properties() {
			private static final long serialVersionUID = 1L;

			@Override
			public synchronized Set<Map.Entry<Object, Object>> entrySet() {
				return Collections.synchronizedSet(super.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).collect(Collectors.toCollection(LinkedHashSet::new)));
			}
		};
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
		synchronized (changedProperties) {
			if (changedProperties.isEmpty()) {
				return;
			}
		}
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

	public Long getLong(String name, Long def) {
		Long result = getLong(name);
		if (result == null) {
			return def;
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

	public List<DeviceConfiguration> getPlutoSdrConfigurations() {
		List<String> loraDevices = getProperties("plutosdr.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("plutosdr.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			String prefix = "plutosdr.device." + cur + ".";
			DeviceConfiguration config = new DeviceConfiguration();
			config.setId("plutosdr");
			config.setName("PlutoSDR");
			config.setTimeout(timeout);
			config.setGain(getDouble(prefix + "gain").floatValue());
			config.setMinimumFrequency(getLong(prefix + "minFrequency"));
			config.setMaximumFrequency(getLong(prefix + "maxFrequency"));
			config.setRotatorConfiguration(getRotatorConfiguration(prefix));
			config.setAntennaConfiguration(getAntennaConfiguration(prefix));
			config.setDeviceType(DeviceType.PLUTOSDR);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getSdrServerConfigurations() {
		List<String> deviceIndices = getProperties("sdrserver.devices");
		if (deviceIndices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("sdrserver.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(deviceIndices.size());
		for (String cur : deviceIndices) {
			DeviceConfiguration config = new DeviceConfiguration();
			String prefix = "sdrserver.device." + cur + ".";
			config.setHost(getProperty(prefix + "host"));
			config.setPort(getInteger(prefix + "port"));
			config.setMinimumFrequency(getLong(prefix + "minFrequency"));
			config.setMaximumFrequency(getLong(prefix + "maxFrequency"));
			config.setTimeout(timeout);
			config.setId("sdrserver-" + config.getHost() + ":" + config.getPort());
			config.setName("SDR-SERVER - " + config.getHost() + ":" + config.getPort());
			config.setRotatorConfiguration(getRotatorConfiguration(prefix + "."));
			config.setAntennaConfiguration(getAntennaConfiguration(prefix + "."));
			SdrServerConfiguration sdrConfig = new SdrServerConfiguration();
			sdrConfig.setBasepath(getProperty(prefix + "basepath"));
			sdrConfig.setBandwidth(getLong("satellites.sdrserver.bandwidth"));
			sdrConfig.setBandwidthCrop(getLong("satellites.sdrserver.bandwidth.crop"));
			sdrConfig.setUseGzip(getBoolean(prefix + "usegzip"));
			config.setSdrServerConfiguration(sdrConfig);
			config.setCompencateDcOffset(false);
			config.setDeviceType(DeviceType.SDRSERVER);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getRtlSdrConfigurations() {
		List<String> sdrDevices = getProperties("rtlsdr.devices");
		if (sdrDevices.isEmpty()) {
			return Collections.emptyList();
		}
		List<DeviceConfiguration> result = new ArrayList<>(sdrDevices.size());
		for (String cur : sdrDevices) {
			DeviceConfiguration config = new DeviceConfiguration();
			String prefix = "rtlsdr.device." + cur + ".";
			config.setMinimumFrequency(getLong(prefix + "minFrequency"));
			config.setMaximumFrequency(getLong(prefix + "maxFrequency"));
			config.setRtlDeviceId(getInteger(prefix + "index"));
			config.setGain(getDouble(prefix + "gain").floatValue());
			config.setBiast(getBoolean(prefix + "biast"));
			config.setPpm(getInteger(prefix + "ppm"));
			config.setRotatorConfiguration(getRotatorConfiguration(prefix));
			config.setAntennaConfiguration(getAntennaConfiguration(prefix));
			config.setId("rtlsdr-" + config.getRtlDeviceId());
			config.setName("RTL-SDR " + config.getRtlDeviceId());
			config.setDeviceType(DeviceType.RTLSDR);
			config.setCompencateDcOffset(true);
			result.add(config);
		}
		return result;
	}

	@Deprecated
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
			config.setHost(getProperty("r2lora.device." + cur + ".host"));
			config.setPort(getInteger("r2lora.device." + cur + ".port"));
			config.setUsername(getProperty("r2lora.device." + cur + ".username"));
			config.setPassword(getProperty("r2lora.device." + cur + ".password"));
			config.setTimeout(timeout);
			config.setId("lora-" + config.getHost() + ":" + config.getPort());
			config.setName("LoRa - " + config.getHost() + ":" + config.getPort());
			config.setRotatorConfiguration(getRotatorConfiguration("r2lora.device." + cur + "."));
			config.setAntennaConfiguration(getAntennaConfiguration("r2lora.device." + cur + "."));
			config.setDeviceType(DeviceType.LORAATWIFI);
			config.setGain(gain);
			config.setCompencateDcOffset(false);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtWifiConfigurations() {
		List<String> loraDevices = getProperties("loraatwifi.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		Integer timeout = getInteger("loraatwifi.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			String prefix = "loraatwifi.device." + cur + ".";
			DeviceConfiguration config = new DeviceConfiguration();
			config.setHost(getProperty(prefix + "host"));
			config.setPort(getInteger(prefix + "port"));
			config.setUsername(getProperty(prefix + "username"));
			config.setPassword(getProperty(prefix + "password"));
			config.setTimeout(timeout);
			String hostport = config.getHost() + ":" + config.getPort();
			config.setId(LORA_AT_DEVICE_PREFIX + hostport);
			config.setName("LoRa - " + hostport);
			config.setRotatorConfiguration(getRotatorConfiguration(prefix));
			config.setAntennaConfiguration(getAntennaConfiguration(prefix));
			Integer gain = getInteger(prefix + "gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			config.setGain(gain);
			Long minFrequency = getLong(prefix + "minFrequency");
			if (minFrequency != null) {
				config.setMinimumFrequency(minFrequency);
			}
			Long maxFrequency = getLong(prefix + "maxFrequency");
			if (maxFrequency != null) {
				config.setMaximumFrequency(maxFrequency);
			}
			config.setCompencateDcOffset(false);
			config.setDeviceType(DeviceType.LORAATWIFI);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getSpyServerConfigurations() {
		List<String> deviceIndices = getProperties("spyserver.devices");
		if (deviceIndices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("spyserver.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(deviceIndices.size());
		for (String cur : deviceIndices) {
			DeviceConfiguration config = new DeviceConfiguration();
			config.setHost(getProperty("spyserver.device." + cur + ".host"));
			config.setPort(getInteger("spyserver.device." + cur + ".port"));
			config.setTimeout(timeout);
			config.setId("spyserver-" + config.getHost() + ":" + config.getPort());
			config.setName("SpyServer - " + config.getHost() + ":" + config.getPort());
			config.setRotatorConfiguration(getRotatorConfiguration("spyserver.device." + cur + "."));
			config.setAntennaConfiguration(getAntennaConfiguration("spyserver.device." + cur + "."));
			Integer gain = getInteger("spyserver.device." + cur + ".gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			config.setGain(gain);
			config.setCompencateDcOffset(false);
			config.setDeviceType(DeviceType.SPYSERVER);
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
			// Yes save port into "host" to be backward compatible
			config.setHost(getProperty("loraat.device." + cur + ".port"));
			config.setTimeout(timeout);
			config.setId("loraat-" + config.getHost());
			config.setName("LoRa - " + config.getHost());
			config.setRotatorConfiguration(getRotatorConfiguration("loraat.device." + cur + "."));
			config.setAntennaConfiguration(getAntennaConfiguration("loraat.device." + cur + "."));
			config.setGain(gain);
			config.setCompencateDcOffset(false);
			config.setDeviceType(DeviceType.LORAAT);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtBleConfigurations() {
		List<String> loraDevices = getProperties("loraatble.devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraatble.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			String prefix = "loraatble.device." + cur + ".";
			Integer gain = getInteger(prefix + "gain");
			if (gain == null) {
				// by default should be auto
				gain = 0;
			}
			DeviceConfiguration config = new DeviceConfiguration();
			String address = getProperty(prefix + "btaddress");
			if (address == null) {
				LOG.error("btaddress is missing for {}", prefix);
				continue;
			}
			config.setHost(address.toLowerCase(Locale.UK));
			config.setTimeout(timeout);
			config.setId(LORA_AT_DEVICE_PREFIX + config.getHost());
			config.setName("LoRa - " + address);
			config.setRotatorConfiguration(getRotatorConfiguration(prefix));
			config.setAntennaConfiguration(getAntennaConfiguration(prefix));
			config.setGain(gain);
			Long minFrequency = getLong(prefix + "minFrequency");
			Long maxFrequency = getLong(prefix + "maxFrequency");
			if (minFrequency == null || maxFrequency == null) {
				LOG.error("min/max frequencies must be specified for {}", prefix);
				continue;
			}
			config.setMinimumFrequency(minFrequency);
			config.setMaximumFrequency(maxFrequency);
			config.setCompencateDcOffset(false);
			Double maxBatteryVoltage = getDouble(prefix + "maxVoltage");
			if (maxBatteryVoltage == null) {
				// assume li-ion
				maxBatteryVoltage = 4.2;
			}
			Double minBatteryVoltage = getDouble(prefix + "minVoltage");
			if (minBatteryVoltage == null) {
				minBatteryVoltage = 3.0;
			}
			config.setMaximumBatteryVoltage(maxBatteryVoltage);
			config.setMinimumBatteryVoltage(minBatteryVoltage);
			config.setDeviceType(DeviceType.LORAATBLE);
			result.add(config);
		}
		return result;
	}

	private AntennaConfiguration getAntennaConfiguration(String prefix) {
		AntennaConfiguration result = new AntennaConfiguration();
		Double minElevation = getDouble(prefix + "antenna.minElevation");
		if (minElevation == null) {
			// old property
			minElevation = getDouble("scheduler.elevation.min");
		}
		result.setMinElevation(minElevation);
		Double guaranteedElevation = getDouble(prefix + "antenna.guaranteedElevation");
		if (guaranteedElevation == null) {
			guaranteedElevation = getDouble("scheduler.elevation.guaranteed");
		}
		result.setGuaranteedElevation(guaranteedElevation);
		String type = getProperty(prefix + "antenna.type");
		if (type != null) {
			result.setType(AntennaType.valueOf(type));
		} else {
			result.setType(AntennaType.OMNIDIRECTIONAL);
		}
		Double azimuth = getDouble(prefix + "antenna.azimuth");
		if (azimuth == null) {
			// default - North
			azimuth = 0.0;
		}
		// orekit expects azimuth in counter clock wise degrees
		result.setAzimuth(FastMath.toRadians(Util.convertAzimuthToDegress(azimuth)));
		Double elevation = getDouble(prefix + "antenna.elevation");
		if (elevation == null) {
			elevation = 0.0;
		} else {
			elevation = FastMath.toRadians(elevation);
		}
		result.setElevation(elevation);
		Double beamwidth = getDouble(prefix + "antenna.beamwidth");
		if (beamwidth == null) {
			beamwidth = 45.0;
		}
		result.setBeamwidth(FastMath.toRadians(beamwidth));
		return result;
	}

	private RotatorConfiguration getRotatorConfiguration(String prefix) {
		boolean enabled = getBoolean(prefix + "rotator.enabled");
		if (!enabled) {
			return null;
		}
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

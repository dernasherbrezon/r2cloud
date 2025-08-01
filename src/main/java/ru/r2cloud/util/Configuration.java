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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.AirspyGainType;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.GeneralConfiguration;
import ru.r2cloud.model.IntegrationConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private final Properties userSettings;
	private final Path userSettingsLocation;
	private final FileSystem fs;
	private static final Set<PosixFilePermission> MODE600 = new HashSet<>();

	private final Properties systemSettings = new Properties();
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
		if (value == null) {
			return null;
		}
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, Float value) {
		if (value == null) {
			return null;
		}
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, Long value) {
		if (value == null) {
			return null;
		}
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, Integer value) {
		if (value == null) {
			return null;
		}
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
		if (value == null) {
			return null;
		}
		synchronized (changedProperties) {
			changedProperties.add(key);
		}
		return (String) userSettings.put(key, value);
	}

	private void removeByPrefix(String prefix) {
		List<String> toRemove = new ArrayList<>();
		for (Entry<Object, Object> cur : userSettings.entrySet()) {
			String key = (String) cur.getKey();
			if (key.startsWith(prefix)) {
				toRemove.add(key);
			}
		}
		synchronized (changedProperties) {
			changedProperties.addAll(toRemove);
		}
		for (String cur : toRemove) {
			userSettings.remove(cur);
		}
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

	public Float getFloat(String name) {
		String str = getProperty(name);
		if (str == null) {
			return null;
		}
		return Float.valueOf(str);
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

	public IntegrationConfiguration getIntegrationConfiguration() {
		IntegrationConfiguration result = new IntegrationConfiguration();
		result.setApiKey(getProperty("r2cloud.apiKey"));
		result.setSyncSpectogram(getBoolean("r2cloud.syncSpectogram"));
		result.setNewLaunch(getBoolean("r2cloud.newLaunches"));
		result.setSatnogs(getBoolean("satnogs.satellites"));
		result.setInfluxdbHostname(getProperty("influxdb.hostname"));
		result.setInfluxdbPort(getInteger("influxdb.port"));
		result.setInfluxdbUsername(getProperty("influxdb.username"));
		result.setInfluxdbPassword(getProperty("influxdb.password"));
		result.setInfluxdbDatabase(getProperty("influxdb.database"));
		return result;
	}

	public void saveIntegrationConfiguration(IntegrationConfiguration config) {
		if (config.getApiKey() != null && config.getApiKey().length() > 0) {
			setProperty("r2cloud.apiKey", config.getApiKey());
		} else {
			remove("r2cloud.apiKey");
		}
		setProperty("r2cloud.syncSpectogram", String.valueOf(config.isSyncSpectogram()));
		setProperty("r2cloud.newLaunches", String.valueOf(config.isNewLaunch()));
		setProperty("satnogs.satellites", String.valueOf(config.isSatnogs()));
		if (config.getInfluxdbHostname() != null && config.getInfluxdbHostname().length() > 0) {
			setProperty("influxdb.hostname", config.getInfluxdbHostname());
		} else {
			remove("influxdb.hostname");
		}
		setProperty("influxdb.port", config.getInfluxdbPort());
		setProperty("influxdb.username", config.getInfluxdbUsername());
		setProperty("influxdb.password", config.getInfluxdbPassword());
		setProperty("influxdb.database", config.getInfluxdbDatabase());
	}

	public GeneralConfiguration getGeneralConfiguration() {
		GeneralConfiguration result = new GeneralConfiguration();
		result.setAlt(getDouble("locaiton.alt"));
		result.setLat(getDouble("locaiton.lat"));
		result.setLng(getDouble("locaiton.lon"));
		result.setLocationAuto(getBoolean("location.auto"));
		result.setPresentationMode(getBoolean("presentationMode"));
		result.setRetentionMaxSizeBytes(getLong("scheduler.data.retention.maxSizeBytes"));
		result.setRetentionRawCount(getInteger("scheduler.data.retention.raw.count"));
		return result;
	}

	public void saveGeneralConfiguration(GeneralConfiguration config) {
		setProperty("location.auto", config.isLocationAuto());
		setProperty("locaiton.lat", config.getLat());
		setProperty("locaiton.lon", config.getLng());
		if (config.getAlt() != null) {
			setProperty("locaiton.alt", config.getAlt());
		} else {
			remove("locaiton.alt");
		}
		setProperty("presentationMode", config.isPresentationMode());
		if (config.getRetentionRawCount() != null) {
			setProperty("scheduler.data.retention.raw.count", config.getRetentionRawCount());
		}
		if (config.getRetentionMaxSizeBytes() != null) {
			setProperty("scheduler.data.retention.maxSizeBytes", config.getRetentionMaxSizeBytes());
		}
	}

	public List<DeviceConfiguration> getPlutoSdrConfigurations() {
		DeviceType deviceType = DeviceType.PLUTOSDR;
		List<String> loraDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("plutosdr.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setName("PlutoSDR");
			config.setTimeout(timeout);
			if (config.getMinimumFrequency() == 0) {
				config.setMinimumFrequency(325_000_000L);
			}
			if (config.getMaximumFrequency() == 0) {
				config.setMaximumFrequency(3_800_000_000L);
			}
			config.setMaximumSampleRate(20_000_000L);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getSdrServerConfigurations() {
		DeviceType deviceType = DeviceType.SDRSERVER;
		List<String> deviceIndices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (deviceIndices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("sdrserver.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(deviceIndices.size());
		for (String cur : deviceIndices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			String prefix = deviceType.name().toLowerCase(Locale.UK) + ".device." + cur + ".";
			SdrServerConfiguration sdrConfig = new SdrServerConfiguration();
			sdrConfig.setBasepath(getProperty(prefix + "basepath"));
			Long bandwidth = getLong(prefix + "bandwidth");
			if (bandwidth == null) {
				bandwidth = getLong("satellites.sdrserver.bandwidth");
			}
			sdrConfig.setBandwidth(bandwidth);
			Long crop = getLong(prefix + "bandwidthCrop");
			if (crop == null) {
				crop = getLong("satellites.sdrserver.bandwidth.crop");
			}
			sdrConfig.setBandwidthCrop(crop);
			sdrConfig.setUseGzip(getBoolean(prefix + "usegzip"));
			config.setSdrServerConfiguration(sdrConfig);
			config.setCompencateDcOffset(false);
			config.setTimeout(timeout);
			config.setName("SDR-SERVER - " + config.getHost() + ":" + config.getPort());
			if (config.getMinimumFrequency() == 0) {
				config.setMinimumFrequency(24_000_000L);
			}
			if (config.getMaximumFrequency() == 0) {
				config.setMaximumFrequency(1_766_000_000L);
			}
			config.setMaximumSampleRate(2_560_000L);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getRtlSdrConfigurations() {
		DeviceType deviceType = DeviceType.RTLSDR;
		List<String> sdrDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (sdrDevices.isEmpty()) {
			return Collections.emptyList();
		}
		List<DeviceConfiguration> result = new ArrayList<>(sdrDevices.size());
		for (String cur : sdrDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setName("RTL-SDR " + config.getRtlDeviceId());
			config.setCompencateDcOffset(true);
			if (config.getMinimumFrequency() == 0) {
				config.setMinimumFrequency(24_000_000L);
			}
			if (config.getMaximumFrequency() == 0) {
				config.setMaximumFrequency(1_766_000_000L);
			}
			config.setMaximumSampleRate(2_560_000L);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getAirspyConfigurations() {
		DeviceType deviceType = DeviceType.AIRSPY;
		List<String> sdrDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (sdrDevices.isEmpty()) {
			return Collections.emptyList();
		}
		List<DeviceConfiguration> result = new ArrayList<>(sdrDevices.size());
		for (String cur : sdrDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setName("AIRSPY " + config.getRtlDeviceId());
			config.setCompencateDcOffset(true);
			if (config.getMinimumFrequency() == 0) {
				config.setMinimumFrequency(24_000_000L);
			}
			if (config.getMaximumFrequency() == 0) {
				config.setMaximumFrequency(1_750_000_000L);
			}
			config.setMaximumSampleRate(6_000_000L);
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
		DeviceType deviceType = DeviceType.LORAATWIFI;
		List<String> loraDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		Integer timeout = getInteger("loraatwifi.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setTimeout(timeout);
			config.setName("LoRa - " + (config.getHost() + ":" + config.getPort()));
			config.setCompencateDcOffset(false);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getSpyServerConfigurations() {
		DeviceType deviceType = DeviceType.SPYSERVER;
		List<String> deviceIndices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (deviceIndices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("spyserver.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(deviceIndices.size());
		for (String cur : deviceIndices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setTimeout(timeout);
			config.setName("SpyServer - " + config.getHost() + ":" + config.getPort());
			config.setCompencateDcOffset(false);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtConfigurations() {
		DeviceType deviceType = DeviceType.LORAAT;
		List<String> loraDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraat.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setTimeout(timeout);
			config.setName("LoRa - " + config.getSerialDevice());
			config.setCompencateDcOffset(false);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtBlecConfigurations() {
		DeviceType deviceType = DeviceType.LORAATBLEC;
		List<String> loraDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraatblec.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setTimeout(timeout);
			config.setName("LoRa - " + config.getBtAddress().toUpperCase());
			config.setCompencateDcOffset(false);
			result.add(config);
		}
		return result;
	}

	public List<DeviceConfiguration> getLoraAtBleConfigurations() {
		DeviceType deviceType = DeviceType.LORAATBLE;
		List<String> loraDevices = getProperties(deviceType.name().toLowerCase(Locale.UK) + ".devices");
		if (loraDevices.isEmpty()) {
			return Collections.emptyList();
		}
		int timeout = getInteger("loraatble.timeout");
		List<DeviceConfiguration> result = new ArrayList<>(loraDevices.size());
		for (String cur : loraDevices) {
			DeviceConfiguration config = getDeviceConfiguration(cur, deviceType);
			config.setTimeout(timeout);
			config.setName("LoRa - " + config.getBtAddress().toUpperCase());
			config.setCompencateDcOffset(false);
			if (config.getMaximumBatteryVoltage() == 0) {
				// assume li-ion
				config.setMaximumBatteryVoltage(4.2);
			}
			if (config.getMinimumBatteryVoltage() == 0) {
				config.setMinimumBatteryVoltage(3.0);
			}
			result.add(config);
		}
		return result;
	}

	public void saveDeviceConfiguration(DeviceConfiguration config) {
		String prefix = indexConfig(config);
		setProperty(prefix + "btaddress", config.getBtAddress());
		setProperty(prefix + "host", config.getHost());
		if (config.getPort() != 0) {
			setProperty(prefix + "port", config.getPort());
		}
		if (config.getMinimumFrequency() != 0) {
			setProperty(prefix + "minFrequency", config.getMinimumFrequency());
		}
		if (config.getMaximumFrequency() != 0) {
			setProperty(prefix + "maxFrequency", config.getMaximumFrequency());
		}
		setProperty(prefix + "index", config.getRtlDeviceId());
		setProperty(prefix + "gain", config.getGain());
		if (config.getGainType() != null) {
			setProperty(prefix + "gainType", config.getGainType().name());
			if (config.getGainType().equals(AirspyGainType.FREE)) {
				setProperty(prefix + "vgaGain", config.getVgaGain());
				setProperty(prefix + "mixerGain", config.getMixerGain());
				setProperty(prefix + "lnaGain", config.getLnaGain());
			}
		}
		setProperty(prefix + "biast", config.isBiast());
		setProperty(prefix + "ppm", config.getPpm());
		setProperty(prefix + "username", config.getUsername());
		setProperty(prefix + "password", config.getPassword());
		setProperty(prefix + "serialDevice", config.getSerialDevice());
		if (config.getMaximumBatteryVoltage() != 0.0) {
			setProperty(prefix + "maxVoltage", config.getMaximumBatteryVoltage());
		}
		if (config.getMinimumBatteryVoltage() != 0.0) {
			setProperty(prefix + "minVoltage", config.getMinimumBatteryVoltage());
		}
		if (config.getSdrServerConfiguration() != null) {
			setProperty(prefix + "basepath", config.getSdrServerConfiguration().getBasepath());
			setProperty(prefix + "bandwidth", config.getSdrServerConfiguration().getBandwidth());
			setProperty(prefix + "bandwidthCrop", config.getSdrServerConfiguration().getBandwidthCrop());
			setProperty(prefix + "usegzip", config.getSdrServerConfiguration().isUseGzip());
		}
		setAntennaConfiguration(prefix, config.getAntennaConfiguration());
		setRotatorConfiguration(prefix, config.getRotatorConfiguration());
	}

	private DeviceConfiguration getDeviceConfiguration(String id, DeviceType deviceType) {
		DeviceConfiguration config = new DeviceConfiguration();
		String deviceTypeLower = deviceType.name().toLowerCase(Locale.UK);
		String prefix = deviceTypeLower + ".device." + id + ".";
		Long minFrequency = getLong(prefix + "minFrequency");
		if (minFrequency != null) {
			config.setMinimumFrequency(minFrequency);
		}
		Long maxFrequency = getLong(prefix + "maxFrequency");
		if (maxFrequency != null) {
			config.setMaximumFrequency(maxFrequency);
		}
		config.setHost(getProperty(prefix + "host"));
		Integer port = getInteger(prefix + "port");
		if (port != null) {
			config.setPort(port);
		}
		config.setUsername(getProperty(prefix + "username"));
		config.setPassword(getProperty(prefix + "password"));
		config.setRtlDeviceId(getProperty(prefix + "index"));
		Double gain = getDouble(prefix + "gain");
		if (gain == null) {
			gain = 0.0;
		}
		config.setGain(gain.floatValue());
		String gainTypeProperty = getProperty(prefix + "gainType");
		if (gainTypeProperty != null) {
			config.setGainType(AirspyGainType.valueOf(gainTypeProperty));
			if (config.getGainType().equals(AirspyGainType.FREE)) {
				config.setVgaGain(getFloat(prefix + "vgaGain"));
				config.setMixerGain(getFloat(prefix + "mixerGain"));
				config.setLnaGain(getFloat(prefix + "lnaGain"));
			}
		}
		config.setSerialDevice(getProperty(prefix + "serialDevice"));
		String address = getProperty(prefix + "btaddress");
		if (address != null) {
			config.setBtAddress(address.toLowerCase(Locale.UK));
		}
		Double maxBatteryVoltage = getDouble(prefix + "maxVoltage");
		if (maxBatteryVoltage != null) {
			config.setMaximumBatteryVoltage(maxBatteryVoltage);
			// assume li-ion
			maxBatteryVoltage = 4.2;
		}
		Double minBatteryVoltage = getDouble(prefix + "minVoltage");
		if (minBatteryVoltage != null) {
			config.setMinimumBatteryVoltage(minBatteryVoltage);
			minBatteryVoltage = 3.0;
		}
		config.setBiast(getBoolean(prefix + "biast"));
		Integer ppm = getInteger(prefix + "ppm");
		if (ppm != null) {
			config.setPpm(ppm);
		}
		config.setRotatorConfiguration(getRotatorConfiguration(prefix));
		config.setAntennaConfiguration(getAntennaConfiguration(prefix));
		config.setId(deviceTypeLower + "." + id);
		config.setDeviceType(deviceType);
		return config;
	}

	public void removeDeviceConfiguration(DeviceConfiguration config) {
		int index = config.getId().lastIndexOf('.');
		if (index == -1) {
			throw new IllegalArgumentException("invalid id format: " + config.getId());
		}
		int deviceIndex = Integer.valueOf(config.getId().substring(index + 1));
		String rootPropName = config.getDeviceType().name().toLowerCase(Locale.UK) + ".devices";
		List<Integer> indexes = getIntegerProperties(rootPropName);
		if (!indexes.remove(Integer.valueOf(deviceIndex))) {
			return;
		}
		StringBuilder newIndexes = new StringBuilder();
		for (int i = 0; i < indexes.size(); i++) {
			if (i != 0) {
				newIndexes.append(',');
			}
			newIndexes.append(indexes.get(i));
		}
		setProperty(rootPropName, newIndexes.toString());
		String prefix = config.getDeviceType().name().toLowerCase(Locale.UK) + ".device." + deviceIndex + ".";
		removeByPrefix(prefix);
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
		result.setAzimuth(azimuth);
		Double elevation = getDouble(prefix + "antenna.elevation");
		if (elevation == null) {
			elevation = 0.0;
		}
		result.setElevation(elevation);
		Double beamwidth = getDouble(prefix + "antenna.beamwidth");
		if (beamwidth == null) {
			beamwidth = 45.0;
		}
		result.setBeamwidth(beamwidth);
		return result;
	}

	private void setAntennaConfiguration(String prefix, AntennaConfiguration config) {
		if (config == null) {
			return;
		}
		setProperty(prefix + "antenna.minElevation", config.getMinElevation());
		setProperty(prefix + "antenna.guaranteedElevation", config.getGuaranteedElevation());
		setProperty(prefix + "antenna.type", config.getType().name());
		setProperty(prefix + "antenna.azimuth", config.getAzimuth());
		setProperty(prefix + "antenna.elevation", config.getElevation());
		setProperty(prefix + "antenna.beamwidth", config.getBeamwidth());
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
			timeout = 10000;
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

	private void setRotatorConfiguration(String prefix, RotatorConfiguration config) {
		if (config == null) {
			return;
		}
		setProperty(prefix + "rotctrld.hostname", config.getHostname());
		setProperty(prefix + "rotctrld.port", config.getPort());
		setProperty(prefix + "rotator.tolerance", config.getTolerance());
		setProperty(prefix + "rotator.cycleMillis", config.getCycleMillis());
	}

	public List<String> getProperties(String name) {
		String rawValue = getProperty(name);
		if (rawValue == null) {
			return Collections.emptyList();
		}
		return Util.splitComma(rawValue);
	}

	public List<Integer> getIntegerProperties(String name) {
		List<String> valueStr = getProperties(name);
		List<Integer> result = new ArrayList<>(valueStr.size());
		for (String cur : valueStr) {
			result.add(Integer.valueOf(cur));
		}
		Collections.sort(result);
		return result;
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

	private String indexConfig(DeviceConfiguration config) {
		String deviceType = config.getDeviceType().name().toLowerCase(Locale.UK);
		String rootPropName = deviceType + ".devices";
		int result;
		if (config.getId() != null) {
			// format is: loraat.0
			int index = config.getId().lastIndexOf('.');
			if (index == -1) {
				throw new IllegalArgumentException("invalid id format: " + config.getId());
			}
			result = Integer.valueOf(config.getId().substring(index + 1));
		} else {
			List<Integer> indexes = getIntegerProperties(rootPropName);
			if (indexes.isEmpty()) {
				result = 0;
			} else {
				result = indexes.get(indexes.size() - 1) + 1;
			}
			indexes.add(result);
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < indexes.size(); i++) {
				if (i != 0) {
					str.append(",");
				}
				str.append(indexes.get(i));
			}
			setProperty(rootPropName, str.toString());

			config.setId(deviceType + "." + result);
		}
		return deviceType + ".device." + result + ".";
	}

	public void remove(String name) {
		synchronized (changedProperties) {
			changedProperties.add(name);
		}
		userSettings.remove(name);
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

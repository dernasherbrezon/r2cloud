package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class DeviceConfiguration {

	private String id;
	private String name;
	private DeviceType deviceType;
	private long minimumFrequency;
	private long maximumFrequency;
	private long maximumSampleRate;

	private String host;
	private int port;
	private int timeout;
	private String username;
	private String password;

	private RotatorConfiguration rotatorConfiguration;
	private AntennaConfiguration antennaConfiguration;

	private String btAddress;
	private float gain;
	private String rtlDeviceId;
	private String serialDevice;
	private boolean biast;
	private boolean compencateDcOffset;
	private int ppm;
	private SdrServerConfiguration sdrServerConfiguration;
	private double maximumBatteryVoltage;
	private double minimumBatteryVoltage;
	private AirspyGainType gainType;
	private float lnaGain;
	private float mixerGain;
	private float vgaGain;

	public AirspyGainType getGainType() {
		return gainType;
	}

	public void setGainType(AirspyGainType gainType) {
		this.gainType = gainType;
	}

	public float getLnaGain() {
		return lnaGain;
	}

	public void setLnaGain(float lnaGain) {
		this.lnaGain = lnaGain;
	}

	public float getMixerGain() {
		return mixerGain;
	}

	public void setMixerGain(float mixerGain) {
		this.mixerGain = mixerGain;
	}

	public float getVgaGain() {
		return vgaGain;
	}

	public void setVgaGain(float vgaGain) {
		this.vgaGain = vgaGain;
	}

	public String getSerialDevice() {
		return serialDevice;
	}

	public void setSerialDevice(String serialDevice) {
		this.serialDevice = serialDevice;
	}

	public String getBtAddress() {
		return btAddress;
	}

	public void setBtAddress(String btAddress) {
		this.btAddress = btAddress;
	}

	public long getMaximumSampleRate() {
		return maximumSampleRate;
	}

	public void setMaximumSampleRate(long maximumSampleRate) {
		this.maximumSampleRate = maximumSampleRate;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public AntennaConfiguration getAntennaConfiguration() {
		return antennaConfiguration;
	}

	public void setAntennaConfiguration(AntennaConfiguration antennaConfiguration) {
		this.antennaConfiguration = antennaConfiguration;
	}

	public double getMaximumBatteryVoltage() {
		return maximumBatteryVoltage;
	}

	public void setMaximumBatteryVoltage(double maximumBatteryVoltage) {
		this.maximumBatteryVoltage = maximumBatteryVoltage;
	}

	public double getMinimumBatteryVoltage() {
		return minimumBatteryVoltage;
	}

	public void setMinimumBatteryVoltage(double minimumBatteryVoltage) {
		this.minimumBatteryVoltage = minimumBatteryVoltage;
	}

	public void setCompencateDcOffset(boolean compencateDcOffset) {
		this.compencateDcOffset = compencateDcOffset;
	}

	public boolean isCompencateDcOffset() {
		return compencateDcOffset;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPpm() {
		return ppm;
	}

	public void setPpm(int ppm) {
		this.ppm = ppm;
	}

	public SdrServerConfiguration getSdrServerConfiguration() {
		return sdrServerConfiguration;
	}

	public void setSdrServerConfiguration(SdrServerConfiguration sdrServerConfiguration) {
		this.sdrServerConfiguration = sdrServerConfiguration;
	}

	public boolean isBiast() {
		return biast;
	}

	public void setBiast(boolean biast) {
		this.biast = biast;
	}

	public String getRtlDeviceId() {
		return rtlDeviceId;
	}

	public void setRtlDeviceId(String rtlDeviceId) {
		this.rtlDeviceId = rtlDeviceId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getMinimumFrequency() {
		return minimumFrequency;
	}

	public void setMinimumFrequency(long minimumFrequency) {
		this.minimumFrequency = minimumFrequency;
	}

	public long getMaximumFrequency() {
		return maximumFrequency;
	}

	public void setMaximumFrequency(long maximumFrequency) {
		this.maximumFrequency = maximumFrequency;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public RotatorConfiguration getRotatorConfiguration() {
		return rotatorConfiguration;
	}

	public void setRotatorConfiguration(RotatorConfiguration rotatorConfiguration) {
		this.rotatorConfiguration = rotatorConfiguration;
	}

	public float getGain() {
		return gain;
	}

	public void setGain(float gain) {
		this.gain = gain;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (id != null) {
			json.add("id", id);
		}
		if (name != null) {
			json.add("name", name);
		}
		if (deviceType != null) {
			json.add("deviceType", deviceType.name());
		}
		if (minimumFrequency != 0) {
			json.add("minimumFrequency", minimumFrequency);
		}
		if (maximumFrequency != 0) {
			json.add("maximumFrequency", maximumFrequency);
		}
		if (host != null) {
			json.add("host", host);
		}
		if (port != 0) {
			json.add("port", port);
		}
		if (username != null) {
			json.add("username", username);
		}
		json.add("gain", gain);
		if (rtlDeviceId != null) {
			json.add("rtlDeviceId", rtlDeviceId);
		}
		json.add("biast", biast);
		json.add("ppm", ppm);
		if (maximumBatteryVoltage != 0) {
			json.add("maximumBatteryVoltage", maximumBatteryVoltage);
		}
		if (minimumBatteryVoltage != 0) {
			json.add("minimumBatteryVoltage", minimumBatteryVoltage);
		}
		if (btAddress != null) {
			json.add("btAddress", btAddress);
		}
		if (serialDevice != null) {
			json.add("serialDevice", serialDevice);
		}
		if (sdrServerConfiguration != null) {
			json.add("bandwidth", sdrServerConfiguration.getBandwidth());
			json.add("bandwidthCrop", sdrServerConfiguration.getBandwidthCrop());
			if (sdrServerConfiguration.getBasepath() != null) {
				json.add("basepath", sdrServerConfiguration.getBasepath());
			}
			json.add("usegzip", sdrServerConfiguration.isUseGzip());
		}
		if (rotatorConfiguration != null) {
			json.add("rotator", rotatorConfiguration.toJson());
		}
		if (antennaConfiguration != null) {
			json.add("antenna", antennaConfiguration.toJson());
		}
		if (gainType != null) {
			json.add("gainType", gainType.name());
			if (gainType.equals(AirspyGainType.FREE)) {
				json.add("lnaGain", lnaGain);
				json.add("mixerGain", mixerGain);
				json.add("vgaGain", vgaGain);
			}
		}
		return json;
	}

	public static DeviceConfiguration fromJson(JsonObject meta) {
		DeviceConfiguration result = new DeviceConfiguration();
		result.setId(meta.getString("id", null));
		result.setName(meta.getString("name", null));
		result.setDeviceType(DeviceType.valueOf(meta.getString("deviceType", "RTLSDR")));
		result.setMinimumFrequency(meta.getLong("minimumFrequency", 0));
		result.setMaximumFrequency(meta.getLong("maximumFrequency", 0));
		result.setHost(meta.getString("host", null));
		result.setPort(meta.getInt("port", 0));
		result.setUsername(meta.getString("username", null));
		result.setGain(meta.getFloat("gain", 0));
		result.setBtAddress(meta.getString("btAddress", null));
		result.setSerialDevice(meta.getString("serialDevice", null));
		JsonValue rtlDeviceId = meta.get("rtlDeviceId");
		if (rtlDeviceId == null || rtlDeviceId.isNull()) {
			result.setRtlDeviceId("0");
		} else {
			if (rtlDeviceId.isNumber()) {
				result.setRtlDeviceId(String.valueOf(rtlDeviceId.asInt()));
			} else {
				result.setRtlDeviceId(rtlDeviceId.asString());
			}
		}
		result.setBiast(meta.getBoolean("biast", false));
		result.setPpm(meta.getInt("ppm", 0));
		result.setMaximumBatteryVoltage(meta.getDouble("maximumBatteryVoltage", 0));
		result.setMinimumBatteryVoltage(meta.getDouble("minimumBatteryVoltage", 0));
		JsonValue bandwidth = meta.get("bandwidth");
		if (bandwidth != null) {
			SdrServerConfiguration sdrConfig = new SdrServerConfiguration();
			sdrConfig.setBandwidth(bandwidth.asLong());
			sdrConfig.setBandwidthCrop(meta.getLong("bandwidthCrop", 0));
			sdrConfig.setBasepath(meta.getString("basepath", null));
			sdrConfig.setUseGzip(meta.getBoolean("usegzip", false));
			result.setSdrServerConfiguration(sdrConfig);
		}
		JsonValue rotator = meta.get("rotator");
		if (rotator != null) {
			result.setRotatorConfiguration(RotatorConfiguration.fromJson(rotator.asObject()));
		}
		JsonValue antenna = meta.get("antenna");
		if (antenna != null) {
			result.setAntennaConfiguration(AntennaConfiguration.fromJson(antenna.asObject()));
		}
		JsonValue gainTypeValue = meta.get("gainType");
		if (gainTypeValue != null) {
			result.setGainType(AirspyGainType.valueOf(gainTypeValue.asString()));
			if (result.getGainType().equals(AirspyGainType.FREE)) {
				result.setLnaGain(meta.getFloat("lnaGain", 0.0f));
				result.setMixerGain(meta.getFloat("mixerGain", 0.0f));
				result.setVgaGain(meta.getFloat("vgaGain", 0.0f));
			}
		}
		return result;
	}
}

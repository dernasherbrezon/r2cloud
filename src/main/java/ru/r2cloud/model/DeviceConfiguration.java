package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;

public class DeviceConfiguration {

	private String id;
	private String name;
	private DeviceType deviceType;
	private long minimumFrequency;
	private long maximumFrequency;

	private String host;
	private int port;
	private int timeout;
	private String username;
	private String password;

	private RotatorConfiguration rotatorConfiguration;
	private AntennaConfiguration antennaConfiguration;

	private float gain;
	private int rtlDeviceId;
	private boolean biast;
	private boolean compencateDcOffset;
	private int ppm;
	private SdrServerConfiguration sdrServerConfiguration;
	private double maximumBatteryVoltage;
	private double minimumBatteryVoltage;

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

	public int getRtlDeviceId() {
		return rtlDeviceId;
	}

	public void setRtlDeviceId(int rtlDeviceId) {
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
		json.add("id", id);
		json.add("name", name);
		json.add("deviceType", deviceType.name());
		if (minimumFrequency != 0) {
			json.add("minimumFrequency", minimumFrequency / 1000000);
		}
		if (maximumFrequency != 0) {
			json.add("maximumFrequency", maximumFrequency / 1000000);
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
		json.add("rtlDeviceId", rtlDeviceId);
		json.add("biast", biast);
		json.add("ppm", ppm);
		if (maximumBatteryVoltage != 0) {
			json.add("maximumBatteryVoltage", maximumBatteryVoltage);
		}
		if (minimumBatteryVoltage != 0) {
			json.add("minimumBatteryVoltage", minimumBatteryVoltage);
		}
		if (sdrServerConfiguration != null) {
			json.add("bandwidth", sdrServerConfiguration.getBandwidth());
			json.add("bandwidthCrop", sdrServerConfiguration.getBandwidthCrop());
			json.add("basepath", sdrServerConfiguration.getBasepath());
			json.add("usegzip", sdrServerConfiguration.isUseGzip());
		}
		if (rotatorConfiguration != null) {
			json.add("rotator", rotatorConfiguration.toJson());
		}
		if (antennaConfiguration != null) {
			json.add("antenna", antennaConfiguration.toJson());
		}
		return json;
	}
}

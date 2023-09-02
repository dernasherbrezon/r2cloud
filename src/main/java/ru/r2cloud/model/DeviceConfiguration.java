package ru.r2cloud.model;

public class DeviceConfiguration {

	private String id;
	private String name;
	private long minimumFrequency;
	private long maximumFrequency;

	private String host;
	private int port;
	private int timeout;
	private String username;
	private String password;

	private RotatorConfiguration rotatorConfiguration;

	private float gain;
	private int rtlDeviceId;
	private boolean biast;
	private boolean compencateDcOffset;
	private int ppm;
	private SdrServerConfiguration sdrServerConfiguration;
	
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
}

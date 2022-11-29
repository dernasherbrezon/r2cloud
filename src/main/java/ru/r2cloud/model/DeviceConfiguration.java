package ru.r2cloud.model;

public class DeviceConfiguration {

	private String id;
	private long minimumFrequency;
	private long maximumFrequency;
	private float gain;

	private String hostport;
	private int timeout;
	private String username;
	private String password;

	private RotatorConfiguration rotatorConfiguration;

	private int rtlDeviceId;
	private boolean biast;
	private SdrServerConfiguration sdrServerConfiguration;
	private int ppm;
	
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

	public String getHostport() {
		return hostport;
	}

	public void setHostport(String hostport) {
		this.hostport = hostport;
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

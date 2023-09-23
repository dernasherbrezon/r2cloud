package ru.r2cloud.model;

public class SdrServerConfiguration {

	private String basepath;
	private long bandwidth;
	private boolean useGzip;
	private long bandwidthCrop;
	
	public long getBandwidthCrop() {
		return bandwidthCrop;
	}
	
	public void setBandwidthCrop(long bandwidthCrop) {
		this.bandwidthCrop = bandwidthCrop;
	}
	
	public long getBandwidth() {
		return bandwidth;
	}
	
	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public String getBasepath() {
		return basepath;
	}

	public void setBasepath(String basepath) {
		this.basepath = basepath;
	}

	public boolean isUseGzip() {
		return useGzip;
	}

	public void setUseGzip(boolean useGzip) {
		this.useGzip = useGzip;
	}

}

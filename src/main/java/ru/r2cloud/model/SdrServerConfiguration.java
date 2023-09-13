package ru.r2cloud.model;

public class SdrServerConfiguration {

	private String basepath;
	private boolean useGzip;

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

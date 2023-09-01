package ru.r2cloud.spyserver;

import java.io.IOException;
import java.io.InputStream;

public class SpyServerDeviceInfo implements CommandResponse {

	private long deviceType;
	private long deviceSerial;
	private long maximumSampleRate;
	private long maximumBandwidth;
	private long decimationStageCount;
	private long gainStageCount;
	private long maximumGainIndex;
	private long minimumFrequency;
	private long maximumFrequency;
	private long resolution;
	private long minimumIQDecimation;
	private long forcedIQFormat;

	public long getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(long deviceType) {
		this.deviceType = deviceType;
	}

	public long getDeviceSerial() {
		return deviceSerial;
	}

	public void setDeviceSerial(long deviceSerial) {
		this.deviceSerial = deviceSerial;
	}

	public long getMaximumSampleRate() {
		return maximumSampleRate;
	}

	public void setMaximumSampleRate(long maximumSampleRate) {
		this.maximumSampleRate = maximumSampleRate;
	}

	public long getMaximumBandwidth() {
		return maximumBandwidth;
	}

	public void setMaximumBandwidth(long maximumBandwidth) {
		this.maximumBandwidth = maximumBandwidth;
	}

	public long getDecimationStageCount() {
		return decimationStageCount;
	}

	public void setDecimationStageCount(long decimationStageCount) {
		this.decimationStageCount = decimationStageCount;
	}

	public long getGainStageCount() {
		return gainStageCount;
	}

	public void setGainStageCount(long gainStageCount) {
		this.gainStageCount = gainStageCount;
	}

	public long getMaximumGainIndex() {
		return maximumGainIndex;
	}

	public void setMaximumGainIndex(long maximumGainIndex) {
		this.maximumGainIndex = maximumGainIndex;
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

	public long getResolution() {
		return resolution;
	}

	public void setResolution(long resolution) {
		this.resolution = resolution;
	}

	public long getMinimumIQDecimation() {
		return minimumIQDecimation;
	}

	public void setMinimumIQDecimation(long minimumIQDecimation) {
		this.minimumIQDecimation = minimumIQDecimation;
	}

	public long getForcedIQFormat() {
		return forcedIQFormat;
	}

	public void setForcedIQFormat(long forcedIQFormat) {
		this.forcedIQFormat = forcedIQFormat;
	}

	@Override
	public void read(InputStream is) throws IOException {
		deviceType = SpyServerClient.readUnsignedInt(is);
		deviceSerial = SpyServerClient.readUnsignedInt(is);
		maximumSampleRate = SpyServerClient.readUnsignedInt(is);
		maximumBandwidth = SpyServerClient.readUnsignedInt(is);
		decimationStageCount = SpyServerClient.readUnsignedInt(is);
		gainStageCount = SpyServerClient.readUnsignedInt(is);
		maximumGainIndex = SpyServerClient.readUnsignedInt(is);
		minimumFrequency = SpyServerClient.readUnsignedInt(is);
		maximumFrequency = SpyServerClient.readUnsignedInt(is);
		resolution = SpyServerClient.readUnsignedInt(is);
		minimumIQDecimation = SpyServerClient.readUnsignedInt(is);
		forcedIQFormat = SpyServerClient.readUnsignedInt(is);
	}

	@Override
	public String toString() {
		return "[deviceType=" + deviceType + ", deviceSerial=" + deviceSerial + ", maximumSampleRate=" + maximumSampleRate + ", maximumBandwidth=" + maximumBandwidth + ", decimationStageCount=" + decimationStageCount + ", gainStageCount=" + gainStageCount + ", maximumGainIndex=" + maximumGainIndex
				+ ", minimumFrequency=" + minimumFrequency + ", maximumFrequency=" + maximumFrequency + ", resolution=" + resolution + ", minimumIQDecimation=" + minimumIQDecimation + ", forcedIQFormat=" + forcedIQFormat + "]";
	}

}

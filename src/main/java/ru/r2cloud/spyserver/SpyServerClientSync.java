package ru.r2cloud.spyserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.r2cloud.jradio.util.LittleEndianDataInputStream;

public class SpyServerClientSync implements CommandResponse {

	private long canControl;
	private long gain;
	private long deviceCenterFrequency;
	private long iQCenterFrequency;
	private long fFTCenterFrequency;
	private long minimumIQCenterFrequency;
	private long maximumIQCenterFrequency;
	private long minimumFFTCenterFrequency;
	private long maximumFFTCenterFrequency;

	public long getCanControl() {
		return canControl;
	}

	public void setCanControl(long canControl) {
		this.canControl = canControl;
	}

	public long getGain() {
		return gain;
	}

	public void setGain(long gain) {
		this.gain = gain;
	}

	public long getDeviceCenterFrequency() {
		return deviceCenterFrequency;
	}

	public void setDeviceCenterFrequency(long deviceCenterFrequency) {
		this.deviceCenterFrequency = deviceCenterFrequency;
	}

	public long getiQCenterFrequency() {
		return iQCenterFrequency;
	}

	public void setiQCenterFrequency(long iQCenterFrequency) {
		this.iQCenterFrequency = iQCenterFrequency;
	}

	public long getfFTCenterFrequency() {
		return fFTCenterFrequency;
	}

	public void setfFTCenterFrequency(long fFTCenterFrequency) {
		this.fFTCenterFrequency = fFTCenterFrequency;
	}

	public long getMinimumIQCenterFrequency() {
		return minimumIQCenterFrequency;
	}

	public void setMinimumIQCenterFrequency(long minimumIQCenterFrequency) {
		this.minimumIQCenterFrequency = minimumIQCenterFrequency;
	}

	public long getMaximumIQCenterFrequency() {
		return maximumIQCenterFrequency;
	}

	public void setMaximumIQCenterFrequency(long maximumIQCenterFrequency) {
		this.maximumIQCenterFrequency = maximumIQCenterFrequency;
	}

	public long getMinimumFFTCenterFrequency() {
		return minimumFFTCenterFrequency;
	}

	public void setMinimumFFTCenterFrequency(long minimumFFTCenterFrequency) {
		this.minimumFFTCenterFrequency = minimumFFTCenterFrequency;
	}

	public long getMaximumFFTCenterFrequency() {
		return maximumFFTCenterFrequency;
	}

	public void setMaximumFFTCenterFrequency(long maximumFFTCenterFrequency) {
		this.maximumFFTCenterFrequency = maximumFFTCenterFrequency;
	}

	@Override
	public void read(InputStream is) throws IOException {
		LittleEndianDataInputStream dis = new LittleEndianDataInputStream(new DataInputStream(is));
		canControl = dis.readUnsignedInt();
		gain = dis.readUnsignedInt();
		deviceCenterFrequency = dis.readUnsignedInt();
		iQCenterFrequency = dis.readUnsignedInt();
		fFTCenterFrequency = dis.readUnsignedInt();
		minimumIQCenterFrequency = dis.readUnsignedInt();
		maximumIQCenterFrequency = dis.readUnsignedInt();
		minimumFFTCenterFrequency = dis.readUnsignedInt();
		maximumFFTCenterFrequency = dis.readUnsignedInt();
	}

}

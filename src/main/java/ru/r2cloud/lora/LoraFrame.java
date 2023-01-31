package ru.r2cloud.lora;

public class LoraFrame {

	private byte[] data;
	private short rssi;
	private float snr;
	private long frequencyError;
	private long timestamp;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public short getRssi() {
		return rssi;
	}

	public void setRssi(short rssi) {
		this.rssi = rssi;
	}

	public float getSnr() {
		return snr;
	}

	public void setSnr(float snr) {
		this.snr = snr;
	}

	public long getFrequencyError() {
		return frequencyError;
	}

	public void setFrequencyError(long frequencyError) {
		this.frequencyError = frequencyError;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "LoraFrame [rssi=" + rssi + ", snr=" + snr + ", frequencyError=" + frequencyError + ", timestamp=" + timestamp + ", dataLength=" + data.length + "]";
	}

}

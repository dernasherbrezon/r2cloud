package ru.r2cloud.lora;

public class LoraFrame {

	private byte[] data;
	private float rssi;
	private float snr;
	private float frequencyError;
	private long timestamp;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public float getRssi() {
		return rssi;
	}

	public void setRssi(float rssi) {
		this.rssi = rssi;
	}

	public float getSnr() {
		return snr;
	}

	public void setSnr(float snr) {
		this.snr = snr;
	}

	public float getFrequencyError() {
		return frequencyError;
	}

	public void setFrequencyError(float frequencyError) {
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

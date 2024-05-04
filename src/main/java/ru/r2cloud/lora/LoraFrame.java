package ru.r2cloud.lora;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class LoraFrame {

	public static final int PROTOCOL_VERSION = 2;

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

	public static LoraFrame read(byte[] value) throws IOException {
		LoraFrame result = new LoraFrame();
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		try (DataInputStream dis = new DataInputStream(bais)) {
			int protocolVersion = dis.readUnsignedByte();
			if (protocolVersion != PROTOCOL_VERSION) {
				return null;
			}
			result.setFrequencyError(dis.readInt());
			result.setRssi(dis.readShort());
			result.setSnr(dis.readFloat());
			result.setTimestamp(dis.readLong());
			int dataLength = dis.readUnsignedShort();
			// max lora packet is 255 bytes
			if (dataLength > 255) {
				return null;
			}
			byte[] data = new byte[dataLength];
			dis.readFully(data);
			result.setData(data);
		}
		return result;
	}

	@Override
	public String toString() {
		return "LoraFrame [rssi=" + rssi + ", snr=" + snr + ", frequencyError=" + frequencyError + ", timestamp=" + timestamp + ", dataLength=" + data.length + "]";
	}

}

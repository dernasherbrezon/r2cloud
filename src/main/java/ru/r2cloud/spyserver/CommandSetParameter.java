package ru.r2cloud.spyserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CommandSetParameter implements CommandRequest {

	private long setting;
	private long value;

	public CommandSetParameter(long setting, long value) {
		super();
		this.setting = setting;
		this.value = value;
	}

	public long getSetting() {
		return setting;
	}

	public void setSetting(long setting) {
		this.setting = setting;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public byte[] toByteArray() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			SpyServerClient.writeUnsignedInt(baos, (int) setting);
			SpyServerClient.writeUnsignedInt(baos, (int) value);
			baos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			return new byte[0];
		}
	}

}

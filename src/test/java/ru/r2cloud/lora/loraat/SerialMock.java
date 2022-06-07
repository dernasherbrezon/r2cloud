package ru.r2cloud.lora.loraat;

import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public class SerialMock implements SerialInterface {

	private final boolean canBeOpened;
	private final InputStream is;
	private final OutputStream os;

	public SerialMock(boolean canBeOpened, InputStream is, OutputStream os) {
		this.canBeOpened = canBeOpened;
		this.is = is;
		this.os = os;
	}

	@Override
	public SerialPortInterface getCommPort(String portDescriptor) throws SerialPortInvalidPortException {
		return new SerialPortMock(canBeOpened, is, os);
	}
}

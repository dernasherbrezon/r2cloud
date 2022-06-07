package ru.r2cloud.lora.loraat;

import java.io.InputStream;
import java.io.OutputStream;

public class SerialPortMock implements SerialPortInterface {

	private final boolean canBeOpened;
	private final InputStream is;
	private final OutputStream os;

	public SerialPortMock(boolean canBeOpened, InputStream is, OutputStream os) {
		this.canBeOpened = canBeOpened;
		this.is = is;
		this.os = os;
	}

	@Override
	public boolean openPort() {
		return canBeOpened;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public boolean setComPortTimeouts(int newTimeoutMode, int newReadTimeout, int newWriteTimeout) {
		return true;
	}

	@Override
	public boolean setBaudRate(int newBaudRate) {
		return true;
	}

	@Override
	public boolean setParity(int newParity) {
		return true;
	}

	@Override
	public boolean setNumDataBits(int newDataBits) {
		return true;
	}

	@Override
	public boolean setNumStopBits(int newStopBits) {
		return true;
	}

	@Override
	public boolean closePort() {
		return true;
	}

}

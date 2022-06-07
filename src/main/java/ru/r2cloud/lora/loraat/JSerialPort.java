package ru.r2cloud.lora.loraat;

import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;

public class JSerialPort implements SerialPortInterface {

	private final SerialPort impl;

	public JSerialPort(SerialPort impl) {
		this.impl = impl;
	}

	@Override
	public boolean setComPortTimeouts(int newTimeoutMode, int newReadTimeout, int newWriteTimeout) {
		return impl.setComPortTimeouts(newTimeoutMode, newReadTimeout, newWriteTimeout);
	}

	@Override
	public boolean setBaudRate(int newBaudRate) {
		return impl.setBaudRate(newBaudRate);
	}

	@Override
	public boolean setParity(int newParity) {
		return impl.setParity(newParity);
	}

	@Override
	public boolean setNumDataBits(int newDataBits) {
		return impl.setNumDataBits(newDataBits);
	}

	@Override
	public boolean setNumStopBits(int newStopBits) {
		return impl.setNumStopBits(newStopBits);
	}

	@Override
	public boolean openPort() {
		return impl.openPort();
	}

	@Override
	public OutputStream getOutputStream() {
		return impl.getOutputStream();
	}

	@Override
	public InputStream getInputStream() {
		return impl.getInputStream();
	}

	@Override
	public boolean closePort() {
		return impl.closePort();
	}

}

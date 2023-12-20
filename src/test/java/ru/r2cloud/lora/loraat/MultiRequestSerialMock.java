package ru.r2cloud.lora.loraat;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public class MultiRequestSerialMock implements SerialInterface {

	private final SerialMock[] mocks;
	private int currentIndex = 0;

	public MultiRequestSerialMock(SerialMock... mocks) {
		this.mocks = mocks;
	}

	@Override
	public SerialPortInterface getCommPort(String portDescriptor) throws SerialPortInvalidPortException {
		SerialPortInterface result = mocks[currentIndex].getCommPort(portDescriptor);
		currentIndex++;
		return result;
	}

}

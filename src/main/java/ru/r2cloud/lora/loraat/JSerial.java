package ru.r2cloud.lora.loraat;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public class JSerial implements SerialInterface {

	@Override
	public SerialPortInterface getCommPort(String portDescriptor) throws SerialPortInvalidPortException {
		return new JSerialPort(SerialPort.getCommPort(portDescriptor));
	}
}

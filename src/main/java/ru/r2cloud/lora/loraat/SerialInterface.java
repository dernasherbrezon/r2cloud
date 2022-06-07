package ru.r2cloud.lora.loraat;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public interface SerialInterface {

	SerialPortInterface getCommPort(String portDescriptor) throws SerialPortInvalidPortException;

}

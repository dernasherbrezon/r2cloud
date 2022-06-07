package ru.r2cloud.lora.loraat;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerialPortInterface {

	boolean setComPortTimeouts(int newTimeoutMode, int newReadTimeout, int newWriteTimeout);

	boolean setBaudRate(int newBaudRate);

	boolean setParity(int newParity);

	boolean setNumDataBits(int newDataBits);

	boolean setNumStopBits(int newStopBits);

	boolean openPort();

	OutputStream getOutputStream();

	InputStream getInputStream();

	boolean closePort();

}

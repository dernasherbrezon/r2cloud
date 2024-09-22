package ru.r2cloud.rotator.allview;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import ru.r2cloud.lora.loraat.SerialInterface;
import ru.r2cloud.lora.loraat.SerialPortInterface;
import ru.r2cloud.rotctrld.Position;

public class AllviewClient {

	private static final Logger LOG = LoggerFactory.getLogger(AllviewClient.class);
	private static final int CHECK_STATUS_DELAY = 100;

	private final String portDescriptor;
	private final int timeout;
	private final SerialInterface serial;

	private SerialPortInterface port;
	private BufferedReader is;
	private BufferedWriter os;

	private AllviewMotor az;
	private AllviewMotor el;

	public AllviewClient(String portDescriptor, int timeout, SerialInterface serial) {
		this.portDescriptor = portDescriptor;
		this.timeout = timeout;
		this.serial = serial;
	}

	public void slew(Position nextPosition, Position previousPosition) throws IOException {
		double currentAzimuth;
		if (az.isFastMode()) {
			try {
				// check if position overrun. i.e.
				currentAzimuth = az.getPosition();
			} catch (IOException e) {
				currentAzimuth = previousPosition.getAzimuth();
			}
		} else {
			currentAzimuth = previousPosition.getAzimuth();
		}
		double azimuthDelta = nextPosition.getAzimuth() - currentAzimuth;
		// counter clock wise
		if (azimuthDelta > 180.0) {
			azimuthDelta = 0 - currentAzimuth - (360.0 - nextPosition.getAzimuth());
		}
		// clock wise
		if (azimuthDelta < -180.0) {
			azimuthDelta = (360.0 - currentAzimuth) + nextPosition.getAzimuth();
		}
		double elevationDelta;
		if (el.isFastMode()) {
			try {
				elevationDelta = nextPosition.getElevation() - el.getPosition();
			} catch (IOException e) {
				elevationDelta = nextPosition.getElevation() - previousPosition.getElevation();
			}
		} else {
			elevationDelta = nextPosition.getElevation() - previousPosition.getElevation();
		}
		double azimuthDeltaAbs = Math.abs(azimuthDelta);
		double elevationDeltaAbs = Math.abs(elevationDelta);

		boolean azStopRequired = az.isStopRequired(azimuthDelta);
		boolean elStopRequired = el.isStopRequired(elevationDelta);
		if (!azStopRequired && !elStopRequired) {
			if (azimuthDeltaAbs > elevationDeltaAbs) {
				az.slew(azimuthDelta);
				el.slew(elevationDelta);
			} else {
				el.slew(elevationDelta);
				az.slew(azimuthDelta);
			}
		} else if (azStopRequired && !elStopRequired) {
			el.slew(elevationDelta);
			az.stop();
			while (!az.waitMotorStop()) {
				try {
					Thread.sleep(CHECK_STATUS_DELAY);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			az.slew(azimuthDelta);
		} else if (!azStopRequired && elStopRequired) {
			az.slew(azimuthDelta);
			el.stop();
			while (!el.waitMotorStop()) {
				try {
					Thread.sleep(CHECK_STATUS_DELAY);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			el.slew(elevationDelta);
		} else {
			az.stop();
			el.stop();
			while (!az.waitMotorStop() || !el.waitMotorStop()) {
				try {
					Thread.sleep(CHECK_STATUS_DELAY);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			if (azimuthDeltaAbs > elevationDeltaAbs) {
				az.slew(azimuthDelta);
				el.slew(elevationDelta);
			} else {
				el.slew(elevationDelta);
				az.slew(azimuthDelta);
			}
		}
	}

	public synchronized void start() throws Exception {
		port = serial.getCommPort(portDescriptor);
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
		port.setBaudRate(9600);
		port.setParity(SerialPort.NO_PARITY);
		port.setNumStopBits(SerialPort.ONE_STOP_BIT);
		if (!port.openPort()) {
			throw new SerialPortInvalidPortException("can't open port");
		}
		is = new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.US_ASCII));
		os = new BufferedWriter(new OutputStreamWriter(port.getOutputStream(), StandardCharsets.US_ASCII));

		az = new AllviewMotor(1, this);
		el = new AllviewMotor(2, this);
	}

	public synchronized void stop() {
		if (az != null) {
			try {
				az.stop();
			} catch (IOException e) {
				LOG.info("can't stop azimuth", e);
			}
		}
		if (el != null) {
			try {
				el.stop();
			} catch (IOException e) {
				LOG.info("can't stop elevation", e);
			}
		}
		if (port != null) {
			port.closePort();
		}
	}

	public synchronized String sendMessage(String message) throws IOException {
		os.append(message);
		os.flush();
		char curChar = '\r';
		while ((curChar = (char) is.read()) != '\r') {
			// skip
		}
		StringBuilder str = new StringBuilder();
		while ((curChar = (char) is.read()) != '\r') {
			str.append(curChar);
		}
		String result = str.toString().trim();
		if (result.startsWith("!")) {
			throw new IOException("invalid response: " + convertErrorCode(Integer.valueOf(result.substring(1))) + " request: " + message);
		}
		// ignore "="
		result = result.substring(1);
		return result;
	}

	private static String convertErrorCode(int code) {
		switch (code) {
		case 0: {
			return "Unknown Command";
		}
		case 1: {
			return "Command Length Error";
		}
		case 2: {
			return "Motor not Stopped";
		}
		case 3: {
			return "Invalid Character";
		}
		case 4: {
			return "Not Initialized";
		}
		case 5: {
			return "Driver Sleeping";
		}
		case 7: {
			return "PEC Training is running";
		}
		case 8: {
			return "No Valid PEC data";
		}
		default:
			return "unknown error code: " + code;
		}
	}

	public Position getPosition() throws IOException {
		Position result = new Position();
		result.setAzimuth(az.getPosition());
		result.setElevation(el.getPosition());
		return result;
	}

	public void setPosition(Position position) throws IOException {
		az.setPosition(position.getAzimuth());
		el.setPosition(position.getElevation());
	}

	public void stopMotors() throws IOException {
		az.stop();
		el.stop();
	}

	public void waitMotorStop() throws IOException {
		while (!az.waitMotorStop() || !el.waitMotorStop()) {
			try {
				Thread.sleep(CHECK_STATUS_DELAY);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	public String getMotorBoardVersion() throws IOException {
		return sendMessage(":e1\r");
	}

}

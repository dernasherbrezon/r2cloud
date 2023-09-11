package ru.r2cloud.lora.loraat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Util;

public class LoraAtSerialClient implements LoraAtClient {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtSerialClient.class);
	private static final Pattern COMMA = Pattern.compile(",");

	private final String portDescriptor;
	private final int timeout;
	private final SerialInterface serial;
	private final Clock clock;

	public LoraAtSerialClient(String portDescriptor, int timeout, SerialInterface serial, Clock clock) {
		this.portDescriptor = portDescriptor;
		this.timeout = timeout;
		this.serial = serial;
		this.clock = clock;
	}

	@Override
	public LoraStatus getStatus() {
		LoraStatus result = new LoraStatus();
		List<String> response;
		try {
			response = sendRequest("AT+CHIP?\r\n");
		} catch (LoraAtException e) {
			LOG.info(e.getMessage());
			result.setDeviceStatus(DeviceConnectionStatus.FAILED);
			return result;
		}
		result.setStatus("IDLE");
		result.setDeviceStatus(DeviceConnectionStatus.CONNECTED);

		List<ModulationConfig> configs = new ArrayList<>();
		for (String cur : response) {
			// interested only in lora parameters
			if (!cur.startsWith("LORA")) {
				continue;
			}
			String[] parts = COMMA.split(cur);
			if (parts.length != 3) {
				LOG.error("malformed response from lora: {}", cur);
				continue;
			}
			ModulationConfig loraConfig = new ModulationConfig();
			loraConfig.setName(parts[0].toLowerCase());
			loraConfig.setMinFrequency(Float.parseFloat(parts[1]));
			loraConfig.setMaxFrequency(Float.parseFloat(parts[2]));
			configs.add(loraConfig);
		}
		result.setConfigs(configs);
		return result;
	}

	@Override
	public LoraResponse startObservation(LoraObservationRequest loraRequest) {
		// make sure lora internal clock is OK
		try {
			sendRequest("AT+TIME=" + (clock.millis() / 1000) + "\r\n");
		} catch (LoraAtException e) {
			return new LoraResponse(e.getMessage());
		}
		LoraResponse result = startObservationImpl(loraRequest);
		if (result.getStatus().equals(ResponseStatus.RECEIVING)) {
			LOG.info("lora-at is already receiving. stopping previous and starting again");
			LoraResponse response = stopObservation();
			if (LOG.isInfoEnabled() && response.getFrames() != null && !response.getFrames().isEmpty()) {
				for (LoraFrame cur : response.getFrames()) {
					LOG.info("previous unknown observation got some data. Logging it here for manual recovery: {}", Arrays.toString(cur.getData()));
				}
			}
			result = startObservationImpl(loraRequest);
		}
		return result;
	}

	private LoraResponse startObservationImpl(LoraObservationRequest loraRequest) {
		LoraResponse result = new LoraResponse();
		String request = "AT+LORARX=" + loraRequest.getFrequency() + "," + loraRequest.getBw() + "," + loraRequest.getSf() + "," + loraRequest.getCr() + "," + loraRequest.getSyncword() + ",10," + loraRequest.getPreambleLength() + "," + loraRequest.getGain() + "," + loraRequest.getLdro() + "\r\n";
		try {
			sendRequest(request);
		} catch (LoraAtException e) {
			String failure = e.getMessage();
			if (failure.contains("already receiving")) {
				result.setStatus(ResponseStatus.RECEIVING);
				return result;
			}
			return new LoraResponse(e.getMessage());
		}
		result.setStatus(ResponseStatus.SUCCESS);
		return result;
	}

	@Override
	public LoraResponse stopObservation() {
		List<String> response;
		try {
			response = sendRequest("AT+STOPRX\r\n");
		} catch (LoraAtException e) {
			return new LoraResponse(e.getMessage());
		}
		LoraResponse result = new LoraResponse();
		result.setStatus(ResponseStatus.SUCCESS);
		if (!response.isEmpty()) {
			List<LoraFrame> frames = new ArrayList<>(response.size());
			for (String cur : response) {
				String[] parts = COMMA.split(cur);
				if (parts.length != 5) {
					LOG.error("malformed response from lora: {}", cur);
					continue;
				}
				LoraFrame curFrame = new LoraFrame();
				curFrame.setData(Util.hexStringToByteArray(parts[0]));
				curFrame.setRssi((short) Float.parseFloat(parts[1]));
				curFrame.setSnr(Float.parseFloat(parts[2]));
				curFrame.setFrequencyError((long) Float.parseFloat(parts[3]));
				curFrame.setTimestamp(Long.parseLong(parts[4]));
				frames.add(curFrame);
			}
			result.setFrames(frames);
		}
		return result;
	}

	private List<String> sendRequest(String request) throws LoraAtException {
		SerialPortInterface port;
		try {
			port = serial.getCommPort(portDescriptor);
		} catch (SerialPortInvalidPortException e) {
			throw new LoraAtException("unable to send request: " + e.getMessage());
		}

		// this is important
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
		// some defaults
		port.setBaudRate(115200);
		port.setParity(SerialPort.NO_PARITY);
		port.setNumDataBits(8);
		port.setNumStopBits(SerialPort.ONE_STOP_BIT);
		if (!port.openPort()) {
			throw new LoraAtException("can't open port: " + portDescriptor);
		}
		try {
			port.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
		} catch (IOException e) {
			if (!port.closePort()) {
				LOG.info("can't close the port");
			}
			throw new LoraAtException("unable to get status: " + e.getMessage());
		}
		try {
			return readResponse(port);
		} catch (IOException e) {
			throw new LoraAtException("unable to read status: " + e.getMessage());
		} finally {
			if (!port.closePort()) {
				LOG.info("can't close the port");
			}
		}
	}

	private static List<String> readResponse(SerialPortInterface port) throws IOException, LoraAtException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.ISO_8859_1))) {
			String curLine = null;
			List<String> result = new ArrayList<>();
			StringBuilder errorMessage = new StringBuilder();
			while ((curLine = reader.readLine()) != null) {
				curLine = curLine.trim();
				LOG.info("response: {}", curLine);
				// skip logging
				if (curLine.charAt(0) == '[') {
					continue;
				}
				if (curLine.equalsIgnoreCase("ERROR")) {
					throw new LoraAtException(errorMessage.toString());
				}
				if (curLine.equalsIgnoreCase("OK")) {
					return result;
				}
				// not clear yet if the response is valid or error message
				// update both
				if (errorMessage.length() > 0) {
					errorMessage.append(": ");
				}
				errorMessage.append(curLine);
				result.add(curLine);
			}
		}
		return Collections.emptyList();
	}

}

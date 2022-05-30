package ru.r2cloud.loraat;

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

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class LoraAtClient {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtClient.class);
	private static final Pattern COMMA = Pattern.compile(",");

	private final String portDescriptor;

	public LoraAtClient(String portDescriptor) {
		this.portDescriptor = portDescriptor;
	}

	public static void main(String[] args) {
		new LoraAtClient(args[0]).getStatus();
	}

	public LoraAtStatus getStatus() {
		LoraAtStatus result = new LoraAtStatus();
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

	public LoraAtResponse startObservation(LoraAtObservationRequest loraRequest) {
		LoraAtResponse result = startObservationImpl(loraRequest);
		if (result.getStatus().equals(ResponseStatus.RECEIVING)) {
			LOG.info("r2lora is already receiving. stopping previous and starting again");
			LoraAtResponse response = stopObservation();
			if (response.getFrames() != null && response.getFrames().size() > 0) {
				for (LoraAtFrame cur : response.getFrames()) {
					LOG.info("previous unknown observation got some data. Logging it here for manual recovery: {}", Arrays.toString(cur.getData()));
				}
			}
			result = startObservation(loraRequest);
		}
		return result;
	}

	private LoraAtResponse startObservationImpl(LoraAtObservationRequest loraRequest) {
		LoraAtResponse result = new LoraAtResponse();
		String request = "AT+LORARX=" + loraRequest.getFrequency() + "," + loraRequest.getBw() + "," + loraRequest.getSf() + "," + loraRequest.getCr() + "," + loraRequest.getSyncword() + ",0," + loraRequest.getPreambleLength() + "," + loraRequest.getGain() + "," + loraRequest.getLdro();
		try {
			sendRequest(request);
		} catch (LoraAtException e) {
			String failure = e.getMessage();
			if (failure.contains("already receiving")) {
				result.setStatus(ResponseStatus.RECEIVING);
				return result;
			}
			return new LoraAtResponse(e.getMessage());
		}
		result.setStatus(ResponseStatus.SUCCESS);
		return result;
	}

	public LoraAtResponse stopObservation() {
		List<String> response;
		try {
			response = sendRequest("AT+STOPRX\r\n");
		} catch (LoraAtException e) {
			return new LoraAtResponse(e.getMessage());
		}
		LoraAtResponse result = new LoraAtResponse();
		result.setStatus(ResponseStatus.SUCCESS);
		if (!response.isEmpty()) {
			List<LoraAtFrame> frames = new ArrayList<>(response.size());
			for (String cur : response) {
				String[] parts = COMMA.split(cur);
				if (parts.length != 5) {
					LOG.error("malformed response from lora: {}", cur);
					continue;
				}
				LoraAtFrame curFrame = new LoraAtFrame();
				curFrame.setData(Util.hexStringToByteArray(parts[0]));
				curFrame.setRssi(Float.parseFloat(parts[1]));
				curFrame.setSnr(Float.parseFloat(parts[2]));
				curFrame.setFrequencyError(Float.parseFloat(parts[3]));
				curFrame.setTimestamp(Long.parseLong(parts[4]));

			}
			result.setFrames(frames);
		}
		return result;
	}

	private List<String> sendRequest(String request) throws LoraAtException {
		SerialPort port = SerialPort.getCommPort(portDescriptor);
		// this is important
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
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

	private static List<String> readResponse(SerialPort port) throws IOException, LoraAtException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.ISO_8859_1))) {
			String curLine = null;
			List<String> result = new ArrayList<>();
			StringBuilder errorMessage = new StringBuilder();
			while ((curLine = reader.readLine()) != null) {
				curLine = curLine.trim();
				// skip logging
				if (curLine.charAt(0) == '[') {
					LOG.info("remote: {}", curLine);
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

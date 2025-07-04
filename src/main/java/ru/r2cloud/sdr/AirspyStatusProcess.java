package ru.r2cloud.sdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class AirspyStatusProcess implements SdrStatusProcess {

	private static final Logger LOG = LoggerFactory.getLogger(AirspyStatusProcess.class);
	private static final String FIRMWARE_PREFIX = "Firmware Version:";
	private static final String SERIAL_NUMBER = "Serial Number:";
	private final Configuration config;
	private final ProcessFactory factory;
	private final String expectedRtlDeviceId;
	private final ReentrantLock lock;
	private SdrStatus previousStatus;
	private List<Long> supportedSampleRates = new ArrayList<>();
	private String serialNumber;

	public AirspyStatusProcess(Configuration config, ProcessFactory factory, String expectedRtlDeviceId, ReentrantLock lock) {
		this.config = config;
		this.factory = factory;
		this.expectedRtlDeviceId = expectedRtlDeviceId;
		this.lock = lock;
		this.previousStatus = getStatus();
	}

	@Override
	public SdrStatus getStatus() {
		try {
			if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
				try {
					previousStatus = getStatusInternally();
					return previousStatus;
				} finally {
					lock.unlock();
				}
			}
			LOG.info("can't get status within specified timeout. returning previous status");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return previousStatus;
	}

	public List<Long> getSupportedSampleRates() {
		return supportedSampleRates;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	private SdrStatus getStatusInternally() {
		SdrStatus result = null;
		try {
			BufferedReader r = null;
			StringBuilder command = new StringBuilder(config.getProperty("satellites.airspy_info.path"));
			if (expectedRtlDeviceId != null) {
				command.append(" -s " + expectedRtlDeviceId);
			}
			ProcessWrapper process = factory.create(command.toString(), false, false);
			r = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String curLine = null;
			List<Long> supportedSampleRates = new ArrayList<>();
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.contains("AIRSPY_ERROR_NOT_FOUND")) {
					result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.FAILED);
					result.setFailureMessage(curLine);
					break;
				} else {
					if (result == null) {
						result = new SdrStatus();
						result.setStatus(DeviceConnectionStatus.CONNECTED);
					}
					int mspsIndex = curLine.indexOf("MSPS");
					if (mspsIndex != -1) {
						supportedSampleRates.add((long) (1_000_000L * Double.valueOf(curLine.substring(0, mspsIndex).trim())));
					}
					int firmwareIndex = curLine.indexOf(FIRMWARE_PREFIX);
					if (firmwareIndex != -1) {
						result.setModel(curLine.substring(FIRMWARE_PREFIX.length()).trim());
					}
					int serialNumberIndex = curLine.indexOf(SERIAL_NUMBER);
					if (serialNumberIndex != -1) {
						this.serialNumber = curLine.substring(SERIAL_NUMBER.length()).trim();
					}
				}
			}
			if (result != null) {
				Collections.sort(supportedSampleRates);
				this.supportedSampleRates = supportedSampleRates;
			}
		} catch (IOException e) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage(e.getMessage());
			Util.logIOException(LOG, "unable to read status", e);
		}
		if (result == null) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage("unable to find device");
		}
		return result;
	}

}

package ru.r2cloud.satellite.reader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SampleRateMapping;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.sdrserver.ResponseStatus;
import ru.r2cloud.sdrserver.SdrServerResponse;
import ru.r2cloud.util.Util;

public class SdrServerReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(SdrServerReader.class);

	private final ObservationRequest req;
	private final DeviceConfiguration deviceConfiguration;
	private final Transmitter transmitter;
	private final CountDownLatch latch = new CountDownLatch(1);

	private Socket socket;

	public SdrServerReader(ObservationRequest req, DeviceConfiguration deviceConfiguration, Transmitter transmitter) {
		this.req = req;
		this.deviceConfiguration = deviceConfiguration;
		this.transmitter = transmitter;
	}

	@Override
	public IQData start() throws InterruptedException {
		File rawFile = null;
		Long startTimeMillis = null;
		Long endTimeMillis = null;

		if (transmitter.getBaudRates() == null) {
			return null;
		}

		Integer maxBaudRate = Collections.max(transmitter.getBaudRates());
		if (maxBaudRate == null) {
			return null;
		}

		SampleRateMapping mapping = Util.getSmallestDividableSampleRate(maxBaudRate, deviceConfiguration.getSdrServerConfiguration().getBandwidth());
		long sampleRate;
		if (mapping != null) {
			sampleRate = mapping.getDeviceOutput();
		} else {
			int rate = (int) (deviceConfiguration.getSdrServerConfiguration().getBandwidth() / maxBaudRate * 3);
			// sample rate guaranteed to be integer dividable from the sdr server bandwidth
			sampleRate = deviceConfiguration.getSdrServerConfiguration().getBandwidth() / rate;
			if (sampleRate % maxBaudRate != 0) {
				LOG.warn("[{}] using non-integer decimation factor for unsupported baud rate: {} and bandwidth: {}", maxBaudRate, deviceConfiguration.getSdrServerConfiguration().getBandwidth());
			}
		}

		try {
			socket = new Socket(deviceConfiguration.getHost(), deviceConfiguration.getPort());
			socket.setSoTimeout(deviceConfiguration.getTimeout());
			OutputStream os = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeByte(0x00); // protocol version
			dos.writeByte(0x00); // type = TYPE_REQUEST
			dos.writeInt((int) req.getFrequency()); // center freq
			dos.writeInt((int) sampleRate); // bandwidth
			dos.writeInt((int) transmitter.getFrequencyBand()); // band frequency
			dos.writeByte(0); // destination=REQUEST_DESTINATION_FILE
			dos.flush();

			InputStream is = socket.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			SdrServerResponse response = new SdrServerResponse(dis);
			if (response.getStatus().equals(ResponseStatus.SUCCESS)) {
				LOG.info("[{}] response from sdr-server: {}", req.getId(), response);
				startTimeMillis = System.currentTimeMillis();
				String basepath = deviceConfiguration.getSdrServerConfiguration().getBasepath();
				if (basepath == null) {
					basepath = System.getenv("TMPDIR");
					if (basepath == null) {
						basepath = "/tmp";
					}
				}
				String path = basepath + File.separator + response.getDetails() + ".cf32";
				if (deviceConfiguration.getSdrServerConfiguration().isUseGzip()) {
					path += ".gz";
				}
				rawFile = new File(path);
				LOG.info("[{}] waiting for results at: {}", req.getId(), path);
				latch.await();
			} else {
				LOG.error("[{}] unable to start: {}", req.getId(), response);
				Util.closeQuietly(socket);
				return null;
			}
		} catch (IOException e) {
			Util.logIOException(LOG, "[" + req.getId() + "] unable to start observation", e);
			Util.closeQuietly(socket);
			return null;
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFile(rawFile);
		result.setDataFormat(DataFormat.COMPLEX_FLOAT);
		result.setSampleRate(sampleRate);
		return result;
	}

	@Override
	public void complete() {
		if (socket == null || socket.isClosed()) {
			return;
		}
		try {
			OutputStream os = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeByte(0x00); // protocol version
			dos.writeByte(0x01); // type = TYPE_SHUTDOWN
			dos.flush();

			// wait until sdr-server release resources and close socket
			// disconnecting too quickly can start next observation
			// the next observation might be in different band, so sdr-server will reject it
			InputStream is = socket.getInputStream();
			while (is.read() != -1) {
				// do nothing
			}
		} catch (IOException e1) {
			if (!socket.isClosed()) {
				Util.logIOException(LOG, "unable to disconnect from sdr-server", e1);
			}
		} finally {
			Util.closeQuietly(socket);
		}
		LOG.info("[{}] disconnected", req.getId());
		latch.countDown();
	}

}

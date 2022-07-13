package ru.r2cloud.satellite.reader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.sdrserver.ResponseStatus;
import ru.r2cloud.sdrserver.SdrServerResponse;
import ru.r2cloud.util.Util;

public class SdrServerReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(SdrServerReader.class);

	private final ObservationRequest req;
	private final CountDownLatch latch = new CountDownLatch(1);

	private Socket socket;

	public SdrServerReader(ObservationRequest req) {
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		File rawFile = null;
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		try {
			socket = new Socket(req.getSdrServerConfiguration().getHost(), req.getSdrServerConfiguration().getPort());
			socket.setSoTimeout(req.getSdrServerConfiguration().getTimeout());
			OutputStream os = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeByte(0x00); // protocol version
			dos.writeByte(0x00); // type = TYPE_REQUEST
			dos.writeInt((int) req.getActualFrequency()); // center freq
			dos.writeInt(req.getSampleRate()); // bandwidth
			dos.writeInt((int) req.getCenterBandFrequency()); // band frequency
			dos.writeByte(0); // destination=REQUEST_DESTINATION_FILE
			dos.flush();

			InputStream is = socket.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			SdrServerResponse response = new SdrServerResponse(dis);
			if (response.getStatus().equals(ResponseStatus.SUCCESS)) {
				LOG.info("[{}] response from sdr-server: {}", req.getId(), response);
				startTimeMillis = System.currentTimeMillis();
				String basepath = req.getSdrServerConfiguration().getBasepath();
				if (basepath == null) {
					basepath = System.getenv("TMPDIR");
					if (basepath == null) {
						basepath = "/tmp";
					}
				}
				String path = basepath + File.separator + response.getDetails() + ".cf32";
				if (req.getSdrServerConfiguration().isUseGzip()) {
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
			LOG.error("[{}] unable to run", req.getId(), e);
			try {
				socket.close();
			} catch (IOException e1) {
				// ignore
			}
			return null;
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFile(rawFile);
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
			try {
				socket.close();
			} catch (IOException e) {
				// ignore
			}
		}
		LOG.info("[{}] disconnected", req.getId());
		latch.countDown();
	}

}

package ru.r2cloud.sdrmodem;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.Context;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.sdrmodem.SdrmodemApi.Response;
import ru.r2cloud.sdrmodem.SdrmodemApi.RxRequest;
import ru.r2cloud.sdrmodem.SdrmodemApi.demod_destination;
import ru.r2cloud.sdrmodem.SdrmodemApi.doppler_settings;
import ru.r2cloud.sdrmodem.SdrmodemApi.file_settings;
import ru.r2cloud.sdrmodem.SdrmodemApi.fsk_demodulation_settings;
import ru.r2cloud.sdrmodem.SdrmodemApi.modem_type;
import ru.r2cloud.sdrmodem.SdrmodemApi.response_status;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SdrModemClient implements ByteInput {

	private static final Logger LOG = LoggerFactory.getLogger(SdrModemClient.class);

	private final String host;
	private final int port;
	private final int timeout;
	private final File rawIq;
	private final ObservationRequest req;
	private final Transmitter transmitter;
	private final Context ctx;
	private final int baudRate;

	private Socket socket = null;
	private DataInputStream dis = null;
	private long framePos = 0;

	public SdrModemClient(Configuration config, File rawIq, ObservationRequest req, Transmitter transmitter, int baudRate) {
		this.host = config.getProperty("sdrmodem.host");
		this.port = config.getInteger("sdrmodem.port");
		this.timeout = config.getInteger("sdrmodem.timeout");
		this.rawIq = rawIq;
		this.req = req;
		this.transmitter = transmitter;
		this.baudRate = baudRate;
		this.ctx = new Context();
		this.ctx.setSoftBits(true);
		// try to restore sample from the input file
		// MM clock sync can skip more or less than decimation
		// shouldn't affect much
		// formula: current symbol * sps * decimation
		this.ctx.setCurrentSample(() -> (long) (framePos * transmitter.getInputSampleRate() / baudRate));
	}

	@Override
	public byte readByte() throws IOException {
		try {
			if (socket == null) {
				SdrMessage message = convert(rawIq, req, transmitter);
				socket = new Socket(host, port);
				socket.setSoTimeout(timeout);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeByte(message.getProtocolVersion());
				dos.writeByte(message.getType().getCode());
				dos.writeInt(message.getMessage().length);
				dos.write(message.getMessage());

				dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				Response response = readResponse(dis);
				if (response == null) {
					throw new IOException("unable to read response");
				}
				if (response.getStatus().equals(response_status.FAILURE)) {
					throw new IOException("unable to start rx: " + response.getDetails());
				}
			}
			framePos++;
			return dis.readByte();
		} catch (SocketTimeoutException e) {
			throw new EOFException();
		}
	}

	private static Response readResponse(DataInputStream dis) throws IOException {
		SdrMessage response = readMessage(dis);
		switch (response.getType()) {
		case RESPONSE:
			return Response.parseFrom(response.getMessage());
		default:
			LOG.info("unknown message type: {}", response.getType());
			return null;
		}
	}

	private static SdrMessage readMessage(DataInputStream dis) throws IOException {
		SdrMessage result = new SdrMessage();
		result.setProtocolVersion(dis.readUnsignedByte());
		result.setType(MessageType.valueOfCode(dis.readUnsignedByte()));
		byte[] data = new byte[dis.readInt()];
		dis.readFully(data);
		result.setMessage(data);
		return result;
	}

	private SdrMessage convert(File rawIq, ObservationRequest req, Transmitter transmitter) {
		fsk_demodulation_settings.Builder fskDemodSettings = fsk_demodulation_settings.newBuilder();
		fskDemodSettings.setDemodFskDeviation(transmitter.getDeviation());
		fskDemodSettings.setDemodFskTransitionWidth((int) transmitter.getTransitionWidth());
		fskDemodSettings.setDemodFskUseDcBlock(true);

		doppler_settings.Builder dopplerSettings = doppler_settings.newBuilder();
		for (String cur : req.getTle().getRaw()) {
			dopplerSettings.addTle(cur);
		}
		dopplerSettings.setLatitude((int) (FastMath.toDegrees(req.getGroundStation().getLatitude()) * 10E6));
		dopplerSettings.setLongitude((int) (FastMath.toDegrees(req.getGroundStation().getLongitude()) * 10E6));
		dopplerSettings.setAltitude((int) (req.getGroundStation().getAltitude() * 10E6));

		file_settings.Builder fs = file_settings.newBuilder();
		fs.setFilename(rawIq.getAbsolutePath());
		fs.setStartTimeSeconds(req.getStartTimeMillis() / 1000);

		RxRequest.Builder b = RxRequest.newBuilder();
		b.setRxSamplingFreq(transmitter.getInputSampleRate());
		b.setRxOffset(0);
		b.setRxCenterFreq(req.getActualFrequency());
		b.setRxDumpFile(false);
		b.setFileSettings(fs.build());
		b.setDemodDestination(demod_destination.SOCKET);
		b.setDemodType(modem_type.GMSK);
		b.setDemodBaudRate(baudRate);
		b.setDemodDecimation(Util.convertDecimation(baudRate));
		b.setFskSettings(fskDemodSettings.build());
		b.setDoppler(dopplerSettings.build());

		SdrMessage result = new SdrMessage();
		result.setProtocolVersion(0);
		result.setType(MessageType.RX_REQUEST);
		result.setMessage(b.build().toByteArray());
		return result;
	}

	@Override
	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}

	@Override
	public Context getContext() {
		return ctx;
	}

}

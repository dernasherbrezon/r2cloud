package ru.r2cloud.sdrmodem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.AssertJson;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.delfipq.DelfiPqBeacon;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.decoder.Decoders;
import ru.r2cloud.satellite.decoder.DopplerCorrectedSource;
import ru.r2cloud.sdrmodem.SdrmodemApi.Response;
import ru.r2cloud.sdrmodem.SdrmodemApi.RxRequest;
import ru.r2cloud.sdrmodem.SdrmodemApi.response_status;
import ru.r2cloud.util.Util;

public class SdrModemClientTest {

	private static final Logger LOG = LoggerFactory.getLogger(SdrModemClientTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SdrModemMock modem;
	private SatelliteDao dao;
	private int serverPort;
	private Decoders decoders;
	private PredictOreKit predict;

	@Test
	public void testSuccess() throws Exception {
		Satellite satellite = dao.findById("51074");
		Transmitter transmitter = satellite.getById("51074-0");
		Observation req = TestUtil.loadObservation("data/delfipq.raw.gz.json");

		final SdrMessage actual = new SdrMessage();
		modem.setHandler(new SdrModemHandler() {

			@Override
			public void handleClient(Socket client) throws IOException {
				DataInputStream dis = new DataInputStream(client.getInputStream());
				actual.setProtocolVersion(dis.readUnsignedByte());
				actual.setType(MessageType.valueOfCode(dis.readUnsignedByte()));
				int messageLength = dis.readInt();
				byte[] payload = new byte[messageLength];
				dis.readFully(payload);
				actual.setMessage(payload);

				RxRequest rx = RxRequest.parseFrom(new ByteArrayInputStream(payload));
				FloatInput next = new DopplerCorrectedSource(predict, new File(rx.getFileSettings().getFilename()), req, transmitter);
				ByteInput input = new FskDemodulator(next, rx.getDemodBaudRate(), transmitter.getDeviation(), Util.convertDecimation(rx.getDemodBaudRate()), transmitter.getTransitionWidth(), true);

				DataOutputStream out = new DataOutputStream(client.getOutputStream());
				out.writeByte(0);
				out.writeByte(MessageType.RESPONSE.getCode());
				Response.Builder response = Response.newBuilder();
				response.setStatus(response_status.SUCCESS);
				response.setDetails(0);
				byte[] output = response.build().toByteArray();
				out.writeInt(output.length);
				out.write(output);

				try {
					while (true) {
						out.writeByte(input.readByte());
					}
				} catch (EOFException e) {
					return;
				} finally {
					input.close();
				}
			}
		});

		File wav = TestUtil.setupClasspathResource(tempFolder, "data/delfipq.raw.gz");
		Decoder decoder = decoders.findByTransmitter(transmitter);
		DecoderResult result = decoder.decode(wav, req, transmitter);
		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
		try (BeaconInputStream<DelfiPqBeacon> source = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), DelfiPqBeacon.class)) {
			assertTrue(source.hasNext());
			AssertJson.assertObjectsEqual("DelfiPqBeacon.json", source.next());
		}

		assertEquals(0, actual.getProtocolVersion());
		assertEquals(MessageType.RX_REQUEST, actual.getType());
	}

	@Before
	public void start() throws Exception {
		startMockServer();
		config = new TestConfiguration(tempFolder);
		config.setProperty("sdrmodem.host", "127.0.0.1");
		config.setProperty("sdrmodem.port", serverPort);
		config.setProperty("sdrmodem.timeout", "1000");
		config.setProperty("satellites.snr", true);
		config.setProperty("satellites.sdr", "SDRSERVER");
		config.setProperty("satellites.demod.GFSK", "SDRMODEM");
		dao = new SatelliteDao(config);
		predict = new PredictOreKit(config);
		decoders = new Decoders(predict, config, null);
	}

	@After
	public void stop() throws Exception {
		if (modem != null) {
			modem.stop();
		}
	}

	private void startMockServer() throws IOException {
		serverPort = 8000;
		for (int i = 0; i < 10; i++) {
			serverPort += i;
			modem = new SdrModemMock(serverPort);
			try {
				modem.start();
				break;
			} catch (BindException e) {
				LOG.info("port: {} taken. trying new", serverPort);
				modem = null;
				continue;
			}
		}
		if (modem == null) {
			throw new RuntimeException("unable to start mock server");
		}
	}

}

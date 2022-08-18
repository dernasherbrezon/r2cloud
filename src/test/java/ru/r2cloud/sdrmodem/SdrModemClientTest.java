package ru.r2cloud.sdrmodem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.sdrmodem.SdrmodemApi.Response;
import ru.r2cloud.sdrmodem.SdrmodemApi.response_status;

public class SdrModemClientTest {

	private static final Logger LOG = LoggerFactory.getLogger(SdrModemClientTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SdrModemMock modem;
	private SdrModemClient client;
	private SatelliteDao dao;
	private int serverPort;

	@Test
	public void testSuccess() throws Exception {
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
				DataOutputStream out = new DataOutputStream(client.getOutputStream());
				out.writeByte(0);
				out.writeByte(MessageType.RESPONSE.getCode());
				Response.Builder response = Response.newBuilder();
				response.setStatus(response_status.SUCCESS);
				response.setDetails(0);
				byte[] output = response.build().toByteArray();
				out.writeInt(output.length);
				out.write(output);
				out.write(new byte[] { (byte) 0xca, (byte) 0xfe });
			}
		});
		File nonExistingFile = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		// just load some observation
		ObservationRequest req = TestUtil.loadObservation("data/aausat.raw.gz.json").getReq();
		Satellite satellite = dao.findById("41460");
		client = new SdrModemClient(config, nonExistingFile, req, satellite.getById("41460-0"), 2400);
		assertEquals(0xca, client.readByte() & 0xFF);
		assertEquals(0xfe, client.readByte() & 0xFF);

		assertEquals(0, actual.getProtocolVersion());
		assertEquals(MessageType.RX_REQUEST, actual.getType());

		try {
			client.readByte();
			fail("eof expected");
		} catch (EOFException e) {
			// ignore
		}
	}

	@Before
	public void start() throws Exception {
		startMockServer();
		config = new TestConfiguration(tempFolder);
		config.setProperty("sdrmodem.host", "127.0.0.1");
		config.setProperty("sdrmodem.port", serverPort);
		config.setProperty("sdrmodem.timeout", "1000");
		dao = new SatelliteDao(config, null, null);
	}

	@After
	public void stop() throws Exception {
		if (client != null) {
			client.close();
		}
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
//		requestHandler = new CollectingRequestHandler("RPRT 0\n");
//		serverMock.setHandler(requestHandler);
	}

}

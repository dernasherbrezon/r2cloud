package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.SteppingClock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.lora.loraat.LoraAtClient;
import ru.r2cloud.lora.loraat.LoraAtSerialClient;
import ru.r2cloud.lora.loraat.MultiStreamInputStream;
import ru.r2cloud.lora.loraat.SerialMock;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;

public class LoraAtReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private LoraAtClient client;
	private Configuration config;

	@Test
	public void testStartFailed() throws Exception {
		client = new LoraAtSerialClient(UUID.randomUUID().toString(), 10000, new SerialMock(false, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()), new SteppingClock(1649679986400L, 1000));
		LoraAtReader reader = new LoraAtReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
	}

	@Test
	public void testStopFailed() throws Exception {
		client = new LoraAtSerialClient(UUID.randomUUID().toString(), 10000, new SerialMock(true, new MultiStreamInputStream("loraat/success.txt", "loraat/success.txt", "loraat/failure.txt"), new ByteArrayOutputStream()), new SteppingClock(1649679986400L, 1000));
		LoraAtReader reader = new LoraAtReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
	}

	@Test
	public void testSuccess() throws Exception {
		client = new LoraAtSerialClient(UUID.randomUUID().toString(), 10000, new SerialMock(true, new MultiStreamInputStream("loraat/success.txt", "loraat/success.txt", "loraat/successStop.txt"), new ByteArrayOutputStream()), new SteppingClock(1649679986400L, 1000));
		LoraAtReader reader = new LoraAtReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNotNull(iqData);
		assertNotNull(iqData.getDataFile());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
	}

	private static Satellite createSatellite() {
		Transmitter transmitter = new Transmitter();
		transmitter.setLoraCodingRate(7);
		transmitter.setLoraPreambleLength(8);
		transmitter.setLoraSpreadFactor(9);
		transmitter.setLoraSyncword(18);
		transmitter.setLoraLdro(0);
		transmitter.setLoraBandwidth(500000);

		Satellite satellite = new Satellite();
		satellite.setId(UUID.randomUUID().toString());
		satellite.setTransmitters(Collections.singletonList(transmitter));
		return satellite;
	}

	private static ObservationRequest createValidRequest() {
		ObservationRequest req = new ObservationRequest();
		req.setActualFrequency(433125000);
		req.setGain(0.0);
		req.setId(UUID.randomUUID().toString());
		return req;
	}

}

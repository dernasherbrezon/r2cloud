package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.ExecuteNowThreadFactory;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.cloud.InfluxDBClient;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.DefaultClock;

public class DecoderServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private DecoderService service;
	private IObservationDao dao;
	private SatelliteDao satelliteDao;
	private Decoders decoders;
	
	@Test
	public void testDeletedBeforeDecodingStarted() throws Exception {
		Decoder decoder = mock(Decoder.class);
		when(decoders.findByTransmitter(any())).thenReturn(decoder);

		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		TestUtil.copy("data/aausat.raw.gz", wav);
		Observation observation = TestUtil.loadObservation("data/aausat.raw.gz.json");
		observation.setStatus(ObservationStatus.RECEIVED);
		dao.insert(observation);
		wav = dao.update(observation, wav);
		
		assertTrue(wav.delete());
		
		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		assertEquals(ObservationStatus.FAILED, observation.getStatus());
	}

	@Test
	public void testScheduleTwice() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		TestUtil.copy("data/aausat.raw.gz", wav);
		Observation observation = TestUtil.loadObservation("data/aausat.raw.gz.json");
		observation.setStatus(ObservationStatus.RECEIVED);
		dao.insert(observation);
		dao.update(observation, wav);

		Decoder decoder = mock(Decoder.class);
		when(decoders.findByTransmitter(any())).thenReturn(decoder);
		DecoderResult firstCall = new DecoderResult();
		firstCall.setNumberOfDecodedPackets(1);
		DecoderResult secondCall = new DecoderResult();
		secondCall.setNumberOfDecodedPackets(2);
		when(decoder.decode(any(), any(), any())).thenReturn(firstCall, secondCall);
		service.retryObservations();
		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		assertNotNull(observation);
		assertEquals(1, observation.getNumberOfDecodedPackets().longValue());
	}

	@Before
	public void start() throws Exception {
		File basepath = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("r2cloud.newLaunches", false);
		config.setProperty("satellites.basepath.location", basepath.getAbsolutePath());
		config.remove("r2cloud.apiKey");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.update();
		satelliteDao = new SatelliteDao(config);
		dao = new ObservationDao(config);
		decoders = mock(Decoders.class);

		service = new DecoderService(config, decoders, dao, new LeoSatDataService(config, dao, null, null), new ExecuteNowThreadFactory(true), new InfluxDBClient(config, new DefaultClock()), satelliteDao);
		service.start();
	}

}

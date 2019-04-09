package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class ObservationResultDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ObservationResultDao dao;

	@Test
	public void testCrud() throws Exception {
		ObservationRequest req = new ObservationRequest();
		req.setActualFrequency(1L);
		req.setSource(FrequencySource.APT);
		req.setEndTimeMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
		req.setEndLatitude(0.2);
		req.setId(UUID.randomUUID().toString());
		req.setInputSampleRate(1);
		req.setOrigin(create());
		req.setOutputSampleRate(1);
		req.setSatelliteFrequency(1);
		req.setActualFrequency(2);
		req.setSatelliteId(UUID.randomUUID().toString());
		req.setStartTimeMillis(System.currentTimeMillis());
		req.setStartLatitude(0.1);
		assertNotNull(dao.insert(req, createTempFile("wav")));
		ObservationFull actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getResult().getWavPath());
		assertEquals(req.getSource(), actual.getReq().getSource());
		assertNull(actual.getResult().getDataPath());
		assertNull(actual.getResult().getaPath());
		assertNull(actual.getResult().getSpectogramPath());
		assertEquals(1, actual.getReq().getSatelliteFrequency());
		assertEquals(2, actual.getReq().getActualFrequency());

		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));

		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("image")));

		assertTrue(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("spectogram")));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getResult().getSpectogramPath());

		ObservationResult res = new ObservationResult();
		res.setChannelA(UUID.randomUUID().toString());
		res.setChannelB(UUID.randomUUID().toString());
		res.setGain(UUID.randomUUID().toString());
		res.setNumberOfDecodedPackets(1L);
		
		ObservationFull full = new ObservationFull(req);
		full.setResult(res);
		assertTrue(dao.update(full));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertEquals(res.getGain(), actual.getResult().getGain());
		assertEquals(res.getNumberOfDecodedPackets(), actual.getResult().getNumberOfDecodedPackets());
		
		List<ObservationFull> all = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(1, all.size());
	}

	private static Satellite create() {
		TLE tle = new TLE(new String[] { "meteor", "1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990", "2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188" });
		return SatelliteFactory.createSatellite(tle);
	}

	private File createTempFile(String data) throws IOException {
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + ".wav");
		try (BufferedWriter w = new BufferedWriter(new FileWriter(result))) {
			w.write(data);
		}
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		dao = new ObservationResultDao(config);
	}
}

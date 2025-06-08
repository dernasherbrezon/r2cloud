package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.satellite.SatelliteDao;

public class SatdumpDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatdumpDecoder decoder;
	private SatelliteDao satelliteDao;
	private TestConfiguration config;

	@Test
	public void testMeteor() throws Exception {
		TestUtil.copyFolder(new File("./src/test/resources/satdump_meteor/").toPath(), tempFolder.getRoot().toPath());
		File raw = new File(tempFolder.getRoot(), "output.raw");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-meteor.json");
		satelliteDao = new SatelliteDao(config);

		Observation req = new Observation();
		req.setId(UUID.randomUUID().toString());
		req.setSampleRate(288_000);
		req.setDataFormat(DataFormat.COMPLEX_UNSIGNED_BYTE);
		Satellite meteormn23 = satelliteDao.findById("57166");
		Transmitter lrpt = meteormn23.getById("57166-0");

		DecoderResult result = decoder.decode(raw, req, lrpt, meteormn23);
		assertNotNull(result.getInstruments());
		assertEquals(1, result.getInstruments().size());
		assertNotNull(result.getData());
		List<Vcdu> data = new ArrayList<>();
		try (BeaconInputStream<Vcdu> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getData())), Vcdu.class)) {
			while (bis.hasNext()) {
				data.add(bis.next());
			}
		}
		assertEquals(2, data.size());
	}

	@Before
	public void start() throws Exception {
		String taskset = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(taskset, new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false));
		ProcessFactoryMock processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("satellites.taskset.path", taskset);
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-noaa.json");

		satelliteDao = new SatelliteDao(config);
		decoder = new SatdumpDecoder(config, processFactory);
	}

}

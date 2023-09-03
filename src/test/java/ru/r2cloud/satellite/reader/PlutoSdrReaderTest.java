package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;

public class PlutoSdrReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private String plutoSdrPath;

	@Test
	public void testFailure() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 0, true)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));

		PlutoSdrReader o = new PlutoSdrReader(config, new DeviceConfiguration(), factory, req, transmitter);
		IQData iqData = o.start();
		o.complete();
		assertNull(iqData.getDataFile());
	}

	@Test
	public void testSuccess() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 143, true)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));

		PlutoSdrReader o = new PlutoSdrReader(config, new DeviceConfiguration(), factory, req, transmitter);
		IQData iqData = o.start();
		o.complete();
		assertNotNull(iqData.getDataFile());
	}

	@Before
	public void start() throws Exception {
		plutoSdrPath = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.plutosdr.wrapper.path", plutoSdrPath);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

	private Map<String, ProcessWrapperMock> create(ProcessWrapperMock pluto) {
		Map<String, ProcessWrapperMock> result = new HashMap<>();
		result.put(plutoSdrPath, pluto);
		return result;
	}
}

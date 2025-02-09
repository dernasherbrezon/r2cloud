package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;

public class SatdumpReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private final String satelliteId = UUID.randomUUID().toString();
	private ObservationRequest req;
	private Transmitter transmitter;
	private TestConfiguration config;
	private String satdumpLocation;

	@Test
	public void testSuccess() throws Exception {
		ProcessFactoryMock factory = new ProcessFactoryMock(Collections.singletonMap(satdumpLocation, new ProcessWrapperMock(null, null, 0, false)), satelliteId);
		SatdumpReader o = new SatdumpReader(config, createDeviceConfiguration(), factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNotNull(iqData.getDataFile());
	}

	@Test
	public void testFailure() throws Exception {
		ProcessFactoryMock factory = new ProcessFactoryMock(Collections.singletonMap(satdumpLocation, new ProcessWrapperMock(null, null, 1, false)), satelliteId);
		SatdumpReader o = new SatdumpReader(config, createDeviceConfiguration(), factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNull(iqData.getDataFile());
	}

	private static DeviceConfiguration createDeviceConfiguration() {
		DeviceConfiguration deviceConfiguration = new DeviceConfiguration();
		deviceConfiguration.setDeviceType(DeviceType.SPYSERVER);
		deviceConfiguration.setBiast(true);
		return deviceConfiguration;
	}

	@Before
	public void start() throws Exception {
		satdumpLocation = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.satdump.path", satdumpLocation);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();

		req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));

		File outputDirectory = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId());
		assertTrue(outputDirectory.mkdirs());
		try (OutputStream cadu = new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, UUID.randomUUID().toString() + ".cadu")))) {
			cadu.write(1);
		}

	}

}

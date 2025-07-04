package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.AirspyGainType;
import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;

public class AirspyReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private String airspyRx;

	@Test
	public void testFailure() throws Exception {
		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.SATDUMP);
		transmitter.setBandwidth(2_400_000L);

		IQData iqData = startReader(1, transmitter);
		assertNull(iqData.getIq());
	}

	@Test
	public void testUnsupportedSampleRate() throws Exception {
		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.SATDUMP);
		transmitter.setBandwidth(7_000_000L);

		assertNull(startReader(0, transmitter));
	}

	@Test
	public void testSuccess() throws Exception {
		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.AX25G3RUH);

		IQData iqData = startReader(0, transmitter);
		assertNotNull(iqData.getIq());
		assertEquals(DataFormat.COMPLEX_SIGNED_SHORT, iqData.getDataFormat());
	}

	private IQData startReader(int statusCode, Transmitter transmitter) throws InterruptedException {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), statusCode, true)), satelliteId);
		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		AirspyReader o = new AirspyReader(config, createDeviceConfig(), factory, req, transmitter, new ReentrantLock(), Collections.singletonList(2_400_000L));
		IQData iqData = o.start();
		o.complete();
		return iqData;
	}

	@Before
	public void start() throws Exception {
		airspyRx = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.airspy_rx.path", airspyRx);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

	private static DeviceConfiguration createDeviceConfig() {
		DeviceConfiguration result = new DeviceConfiguration();
		result.setMaximumSampleRate(2400000L);
		result.setGainType(AirspyGainType.SENSITIVE);
		result.setGain(4.0f);
		return result;
	}

	private Map<String, ProcessWrapperMock> create(ProcessWrapperMock rtl) {
		Map<String, ProcessWrapperMock> result = new HashMap<>();
		result.put(airspyRx, rtl);
		return result;
	}
}

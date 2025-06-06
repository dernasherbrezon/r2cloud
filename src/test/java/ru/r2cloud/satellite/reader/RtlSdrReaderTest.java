package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
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
import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;

public class RtlSdrReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private String rtlsdrWrapper;
	private String rtlBiast;
	private String rtlsdr;

	@Test
	public void testBiasTSuccess() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 143, true), new ProcessWrapperMock(null, null, 0)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		DeviceConfiguration deviceConfiguration = createDeviceConfig();
		deviceConfiguration.setBiast(true);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.AX25G3RUH);

		RtlSdrReader o = new RtlSdrReader(config, deviceConfiguration, factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNotNull(iqData.getIq());
	}

	@Test
	public void testBiasTFailure() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 143, true), new ProcessWrapperMock(null, null, 1)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		DeviceConfiguration deviceConfiguration = createDeviceConfig();
		deviceConfiguration.setBiast(true);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.AX25G3RUH);

		RtlSdrReader o = new RtlSdrReader(config, deviceConfiguration, factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNull(iqData);
	}

	@Test
	public void testFailure() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 0)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.AX25G3RUH);

		RtlSdrReader o = new RtlSdrReader(config, createDeviceConfig(), factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNull(iqData.getIq());
	}

	@Test
	public void testSuccess() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessFactoryMock factory = new ProcessFactoryMock(create(new ProcessWrapperMock(null, null, 143, true)), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.AX25G3RUH);

		RtlSdrReader o = new RtlSdrReader(config, createDeviceConfig(), factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNotNull(iqData.getIq());
		assertEquals(DataFormat.COMPLEX_UNSIGNED_BYTE, iqData.getDataFormat());
	}

	@Test
	public void testSatdumpSuccess() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		ProcessWrapperMock mock = new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), null, new ByteArrayInputStream(new byte[0]), 0, true);
		ProcessFactoryMock factory = new ProcessFactoryMock(create(mock), satelliteId);

		ObservationRequest req = new ObservationRequest();
		req.setSatelliteId(satelliteId);

		Transmitter transmitter = new Transmitter();
		transmitter.setBaudRates(Collections.singletonList(9600));
		transmitter.setFraming(Framing.SATDUMP);

		RtlSdrReader o = new RtlSdrReader(config, createDeviceConfig(), factory, req, transmitter, new ReentrantLock());
		IQData iqData = o.start();
		o.complete();
		assertNotNull(iqData.getIq());
		assertEquals(DataFormat.COMPLEX_UNSIGNED_BYTE, iqData.getDataFormat());
	}

	@Before
	public void start() throws Exception {
		rtlsdrWrapper = UUID.randomUUID().toString();
		rtlsdr = UUID.randomUUID().toString();
		rtlBiast = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.rtlsdrwrapper.path", rtlsdrWrapper);
		config.setProperty("satellites.rtlsdr.path", rtlsdr);
		config.setProperty("satellites.rtlsdr.biast.path", rtlBiast);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

	private static DeviceConfiguration createDeviceConfig() {
		DeviceConfiguration result = new DeviceConfiguration();
		result.setMaximumSampleRate(2400000L);
		return result;
	}

	private Map<String, ProcessWrapperMock> create(ProcessWrapperMock rtl) {
		return create(rtl, null);
	}

	private Map<String, ProcessWrapperMock> create(ProcessWrapperMock rtl, ProcessWrapperMock bias) {
		Map<String, ProcessWrapperMock> result = new HashMap<>();
		result.put(rtlsdrWrapper, rtl);
		result.put(rtlsdr, rtl);
		result.put(rtlBiast, bias);
		return result;
	}

}

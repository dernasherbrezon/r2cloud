package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyClientSync;
import ru.r2cloud.spyclient.SpyServerDeviceInfo;
import ru.r2cloud.spyclient.SpyServerParameter;

public class SpyServerReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private DeviceConfiguration deviceConfiguration;
	private SpyServerMock mock;

	@Test
	public void testDeviceNotConnected() throws Exception {
		SpyServerReader reader = new SpyServerReader(config, createValidRequest(), deviceConfiguration, null);
		reader.complete();
		assertNull(reader.start());
	}

	@Test
	public void testSuccess() throws Exception {
		mock.setDeviceInfo(createAirSpy());
		mock.setSync(createValidSync());
		byte[] data = createSample();
		mock.setData(data, SpyClient.SPYSERVER_MSG_TYPE_INT16_IQ);

		ObservationRequest req = createValidRequest();
		SpyServerReader reader = new SpyServerReader(config, req, deviceConfiguration, createValidTransmitter());
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					mock.waitForDataSent();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				reader.complete();
			}
		}).start();
		IQData result = reader.start();
		assertNotNull(result);
		assertEquals(DataFormat.COMPLEX_SIGNED_SHORT, result.getDataFormat());
		assertEquals(93750, result.getInputSampleRate());
		assertEquals(93750, result.getOutputSampleRate());
		assertNotNull(result.getDataFile());
		assertEquals(req.getFrequency(), mock.getParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FREQUENCY).longValue());
		assertEquals((long) deviceConfiguration.getGain(), mock.getParameter(SpyServerParameter.SPYSERVER_SETTING_GAIN).longValue());
	}

	@Before
	public void start() throws Exception {
		deviceConfiguration = new DeviceConfiguration();
		deviceConfiguration.setHost("localhost");
		deviceConfiguration.setPort(8008);
		deviceConfiguration.setTimeout(1000);
		deviceConfiguration.setGain(10.0f);
		mock = new SpyServerMock(deviceConfiguration.getHost(), deviceConfiguration.getPort());
		mock.start();

		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

	private static byte[] createSample() {
		byte[] result = new byte[20];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) i;
		}
		return result;
	}

	public static SpyServerDeviceInfo createAirSpy() {
		SpyServerDeviceInfo result = new SpyServerDeviceInfo();
		result.setDeviceType(1);
		result.setMaximumSampleRate(6000000);
		result.setMinimumIQDecimation(0);
		result.setResolution(12);
		result.setDecimationStageCount(12);
		result.setMinimumFrequency(24_000_000L);
		result.setMaximumFrequency(1_700_000_000L);
		return result;
	}

	private static Transmitter createValidTransmitter() {
		Transmitter result = new Transmitter();
		List<Integer> baudRates = new ArrayList<>();
		baudRates.add(1200);
		baudRates.add(9600);
		result.setBaudRates(baudRates);
		return result;
	}

	private static ObservationRequest createValidRequest() {
		ObservationRequest result = new ObservationRequest();
		result.setId(UUID.randomUUID().toString());
		result.setSatelliteId(UUID.randomUUID().toString());
		result.setFrequency(434000000);
		return result;
	}

	public static SpyClientSync createValidSync() {
		SpyClientSync result = new SpyClientSync();
		result.setCanControl(1);
		return result;
	}

	@After
	public void stop() {
		if (mock != null) {
			mock.stop();
		}
	}
}

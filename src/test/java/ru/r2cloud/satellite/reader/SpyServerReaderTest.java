package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
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
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyClientSync;
import ru.r2cloud.spyclient.SpyServerDeviceInfo;
import ru.r2cloud.spyclient.SpyServerDeviceType;
import ru.r2cloud.spyclient.SpyServerParameter;

public class SpyServerReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private DeviceConfiguration deviceConfiguration;
	private SpyServerMock mock;

	@Test
	public void testDeviceNotConnected() throws Exception {
		mock.setDeviceInfo(null);
		SpyServerReader reader = new SpyServerReader(config, createValidRequest(), deviceConfiguration, createValidTransmitter(), new ReentrantLock());
		reader.complete();
		assertNull(reader.start());
	}

	@Test
	public void testSatdump() throws Exception {
		ObservationRequest req = createValidRequest();
		Transmitter transmitter = createValidTransmitter();
		// some generic satdump-based satellite info
		transmitter.setBandwidth(2_400_000L);
		transmitter.setFraming(Framing.SATDUMP);
		SpyServerReader reader = new SpyServerReader(config, req, deviceConfiguration, transmitter, new ReentrantLock());
		IQData result = syncRead(reader);
		assertNotNull(result);
		assertNotNull(result.getIq());
	}

	@Test
	public void testSuccess() throws Exception {
		ObservationRequest req = createValidRequest();
		SpyServerReader reader = new SpyServerReader(config, req, deviceConfiguration, createValidTransmitter(), new ReentrantLock());
		IQData result = syncRead(reader);
		assertNotNull(result);
		assertEquals(DataFormat.COMPLEX_SIGNED_SHORT, result.getDataFormat());
		assertEquals(46875, result.getSampleRate());
		assertNotNull(result.getIq());
		assertEquals(req.getFrequency(), mock.getParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FREQUENCY).longValue());
		assertEquals((long) deviceConfiguration.getGain(), mock.getParameter(SpyServerParameter.SPYSERVER_SETTING_GAIN).longValue());
	}

	@Before
	public void start() throws Exception {
		mock = new SpyServerMock("localhost");
		mock.start();
		mock.setDeviceInfo(createAirSpy());
		mock.setSync(createValidSync());
		mock.setData(createSample(), SpyClient.SPYSERVER_MSG_TYPE_INT16_IQ);

		deviceConfiguration = new DeviceConfiguration();
		deviceConfiguration.setHost("localhost");
		deviceConfiguration.setTimeout(1000);
		deviceConfiguration.setGain(10.0f);
		deviceConfiguration.setPort(mock.getPort());

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
		result.setDeviceType(SpyServerDeviceType.AIRSPY_ONE);
		result.setDeviceSerial(2836433);
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

	private IQData syncRead(SpyServerReader reader) throws InterruptedException {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					// make sure all config parameters are sent/read
					mock.waitForDataSent();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				reader.complete();
			}
		}).start();
		IQData result = reader.start();
		return result;
	}

	@After
	public void stop() {
		if (mock != null) {
			mock.stop();
		}
	}
}

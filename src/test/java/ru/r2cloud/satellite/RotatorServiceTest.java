package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.bodies.GeodeticPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.CollectingRequestHandler;
import ru.r2cloud.RotctrldMock;
import ru.r2cloud.ScheduleFixedTimesTheadPoolFactory;
import ru.r2cloud.SequentialRequestHandler;
import ru.r2cloud.SimpleRequestHandler;
import ru.r2cloud.SteppingClock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class RotatorServiceTest {

	private static final Pattern SPACE = Pattern.compile(" ");
	private static final Logger LOG = LoggerFactory.getLogger(RotatorServiceTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private PredictOreKit predict;
	private RotctrldMock serverMock;
	private RotatorService service;
	private int serverPort;
	private CollectingRequestHandler requestHandler;

	@Test
	public void testStaleTle() throws Exception {
		ObservationRequest req = createRequest();
		req.getTle().getRaw()[1] = "1 53376U 22096G   24125.30500733  .01222779  97906-3  12443-2 0  9998";
		req.getTle().getRaw()[2] = "2 53376  97.3733  46.7756 0008493 160.3534 199.8078 16.11052950 97692";
		req.setStartTimeMillis(1725198038000L);
		req.setEndTimeMillis(1725199038000L);
		int times = (int) ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 70000);
		service = new RotatorService(createValidConfig(), predict, new ScheduleFixedTimesTheadPoolFactory(times), new SteppingClock(req.getStartTimeMillis(), 70000));
		service.start();
		try {
			assertNotNull(service.schedule(req, 1725198028000L, null));
		} catch (RuntimeException e) {
			assertEquals("negative elevation", e.getMessage());
		}
		assertPositions("expected/rotctrld-requests-stale-tle.txt", requestHandler.getRequests());
	}

	@Test
	public void testRotctrldDuringPeriodOfUnavailability() {
		SequentialRequestHandler handler = new SequentialRequestHandler(new SimpleRequestHandler("test\n"), new SimpleRequestHandler("RPRT 0\n"), new SimpleRequestHandler("RPRT 1\n"), new SimpleRequestHandler("test\n"), new SimpleRequestHandler("RPRT 0\n"));
		serverMock.setHandler(handler);
		DefaultClock clock = new DefaultClock();
		ObservationRequest req = createRequest();
		service = new RotatorService(createValidConfig(), predict, new ScheduleFixedTimesTheadPoolFactory(3), new SteppingClock(req.getStartTimeMillis(), 70000));
		service.start();
		service.schedule(req, clock.millis(), null);
		int i = 0;
		List<String> requests = handler.getRequests();
		assertEquals("\\get_info", requests.get(i++));
		assertPosition("\\set_pos 13.012740887809334 4.795259842441547", requests.get(i++));
		assertPosition("\\set_pos 13.088623261159723 10.736921202289226", requests.get(i++));
		assertEquals("\\get_info", requests.get(i++));
		assertPosition("\\set_pos 13.02476416487044 18.929574822190133", requests.get(i++));
	}

	@Test(expected = RuntimeException.class)
	public void testRunPastObservationEnd() {
		ObservationRequest req = createRequest();
		int times = (int) ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000);
		service = new RotatorService(createValidConfig(), predict, new ScheduleFixedTimesTheadPoolFactory(times), new SteppingClock(req.getEndTimeMillis() + 1000, 1000));
		service.start();
		service.schedule(req, req.getStartTimeMillis(), null);
	}

	@Test
	public void testScheduleRotationEvenIfRotatorNotYetEnabled() {
		DefaultClock clock = new DefaultClock();
		RotatorConfiguration rotatorConfiguration = createValidConfig();
		rotatorConfiguration.setPort(rotatorConfiguration.getPort() + 1);
		service = new RotatorService(rotatorConfiguration, predict, new ThreadPoolFactoryImpl(10000), clock);
		service.start();
		assertNotNull(service.schedule(createRequest(), clock.millis(), null));
	}

	@Test(expected = RuntimeException.class)
	public void testDoNotRunIfSdrCancelled() throws Exception {
		Future<?> rtlsdrFuture = mock(Future.class);
		when(rtlsdrFuture.isDone()).thenReturn(true);

		ObservationRequest req = createRequest();
		int times = (int) ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000);
		service = new RotatorService(createValidConfig(), predict, new ScheduleFixedTimesTheadPoolFactory(times), new SteppingClock(req.getStartTimeMillis(), 1000));
		service.start();
		service.schedule(req, req.getStartTimeMillis(), rtlsdrFuture);
	}

	@Test
	public void testSuccess() throws Exception {
		ObservationRequest req = createRequest();
		int times = (int) ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000);
		service = new RotatorService(createValidConfig(), predict, new ScheduleFixedTimesTheadPoolFactory(times), new SteppingClock(req.getStartTimeMillis(), 1000));
		service.start();
		assertNotNull(service.schedule(req, req.getStartTimeMillis(), null));
		assertPositions("expected/rotctrld-requests.txt", requestHandler.getRequests());
	}

	private static void assertPositions(String expectedFile, List<String> requests) throws Exception {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(RotatorServiceTest.class.getClassLoader().getResourceAsStream(expectedFile), StandardCharsets.UTF_8))) {
			String curLine = null;
			int i = 0;
			while ((curLine = r.readLine()) != null) {
				assertPosition(curLine, requests.get(i));
				i++;
			}
			assertEquals(i, requests.size());
		}
	}

	private static void assertPosition(String expected, String actual) {
		String[] expectedParts = SPACE.split(expected);
		String[] actualParts = SPACE.split(actual);
		assertEquals(Double.valueOf(expectedParts[1]), Double.valueOf(actualParts[1]), 0.0001);
		assertEquals(Double.valueOf(expectedParts[2]), Double.valueOf(actualParts[2]), 0.0001);
	}

	private ObservationRequest createRequest() {
		Tle tle = new Tle(new String[] { "funcube-1", "1 39444U 13066AE  20157.75071106  .00000221  00000-0  33451-4 0  9997", "2 39444  97.5589 158.6491 0056696 309.6463  49.9756 14.82127945351637" });
		ObservationRequest result = new ObservationRequest();
		result.setTle(tle);
		result.setStartTimeMillis(getTime("2020-06-06 05:50:29"));
		result.setEndTimeMillis(getTime("2020-06-06 06:01:42"));
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		result.setGroundStation(new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0));
		return result;
	}

	private static long getTime(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return sdf.parse(str).getTime();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void stop() {
		if (service != null) {
			service.stop();
		}
		if (serverMock != null) {
			serverMock.stop();
		}
	}

	@Before
	public void start() throws Exception {
		startMockServer();
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.721");
		config.setProperty("locaiton.lon", "5.030");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		predict = new PredictOreKit(config);
	}

	private RotatorConfiguration createValidConfig() {
		RotatorConfiguration config = new RotatorConfiguration();
		config.setId(UUID.randomUUID().toString());
		config.setHostname("127.0.0.1");
		config.setPort(serverPort);
		config.setCycleMillis(1000);
		config.setTimeout(10000);
		config.setTolerance(5);
		return config;
	}

	private void startMockServer() throws IOException {
		serverPort = 8000;
		for (int i = 0; i < 10; i++) {
			serverPort += i;
			serverMock = new RotctrldMock(serverPort);
			try {
				serverMock.start();
				break;
			} catch (BindException e) {
				LOG.info("port: {} taken. trying new", serverPort);
				serverMock = null;
				continue;
			}
		}
		if (serverMock == null) {
			throw new RuntimeException("unable to start mock server");
		}
		requestHandler = new CollectingRequestHandler("RPRT 0\n");
		serverMock.setHandler(requestHandler);
	}

}

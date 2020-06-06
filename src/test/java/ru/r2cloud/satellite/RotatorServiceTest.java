package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
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

	@Test(expected = RuntimeException.class)
	public void testRotctrldUnavailableDuringThePass() {
		serverMock.setHandler(new SequentialRequestHandler(new SimpleRequestHandler("test\n"), new SimpleRequestHandler("RPRT 1\n")));
		config.setProperty("rotator.enabled", true);
		DefaultClock clock = new DefaultClock();
		ObservationRequest req = createRequest();
		service = new RotatorService(config, predict, new ScheduleFixedTimesTheadPoolFactory(1), new SteppingClock(req.getStartTimeMillis(), 1000));
		service.start();
		service.schedule(req, clock.millis());
	}

	@Test
	public void disableEnableService() {
		config.setProperty("rotator.enabled", true);
		DefaultClock clock = new DefaultClock();
		service = new RotatorService(config, predict, new ScheduleFixedTimesTheadPoolFactory(0), clock);
		service.start();
		config.setProperty("rotator.enabled", false);
		config.update();
		assertNull(service.schedule(createRequest(), clock.millis()));
		config.setProperty("rotator.enabled", true);
		config.update();
		assertNotNull(service.schedule(createRequest(), clock.millis()));
	}

	@Test
	public void testServiceDisabledIfRotctrldIsUnavailable() {
		config.setProperty("rotator.enabled", true);
		config.setProperty("rotator.rotctrld.port", serverPort + 1);
		DefaultClock clock = new DefaultClock();
		service = new RotatorService(config, predict, new ThreadPoolFactoryImpl(), clock);
		service.start();
		assertNull(service.schedule(createRequest(), clock.millis()));
	}

	@Test
	public void testSuccess() throws Exception {
		ObservationRequest req = createRequest();
		int times = (int) ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000);
		config.setProperty("rotator.enabled", true);
		service = new RotatorService(config, predict, new ScheduleFixedTimesTheadPoolFactory(times), new SteppingClock(req.getStartTimeMillis(), 1000));
		service.start();
		assertNotNull(service.schedule(req, req.getStartTimeMillis()));
		try (BufferedReader r = new BufferedReader(new InputStreamReader(RotatorService.class.getClassLoader().getResourceAsStream("expected/rotctrld-requests.txt"), StandardCharsets.UTF_8))) {
			String curLine = null;
			int i = 0;
			while ((curLine = r.readLine()) != null) {
				assertPosition(curLine, requestHandler.getRequests().get(i));
				i++;
			}
			assertEquals(i, requestHandler.getRequests().size());
		}
	}

	private static void assertPosition(String expected, String actual) {
		String[] expectedParts = SPACE.split(expected);
		String[] actualParts = SPACE.split(actual);
		assertEquals(Double.valueOf(expectedParts[1]), Double.valueOf(actualParts[1]), 0.0001);
		assertEquals(Double.valueOf(expectedParts[2]), Double.valueOf(actualParts[2]), 0.0001);
	}
	

	@Test
	public void testDisabled() {
		config.setProperty("rotator.enabled", false);
		DefaultClock clock = new DefaultClock();
		service = new RotatorService(config, predict, new ThreadPoolFactoryImpl(), clock);
		service.start();
		assertNull(service.schedule(createRequest(), clock.millis()));
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
		config.setProperty("rotator.rotctrld.hostname", "127.0.0.1");
		config.setProperty("rotator.rotctrld.port", serverPort);
		config.setProperty("rotator.cycleMillis", 1000);
		config.setProperty("locaiton.lat", "51.721");
		config.setProperty("locaiton.lon", "5.030");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		predict = new PredictOreKit(config);
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

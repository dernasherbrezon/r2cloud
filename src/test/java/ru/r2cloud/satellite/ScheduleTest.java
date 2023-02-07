package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.FixedClock;
import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.LeoSatDataServerMock;
import ru.r2cloud.SatnogsServerMock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.cloud.SatnogsClient;
import ru.r2cloud.device.Device;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.Housekeeping;
import ru.r2cloud.tle.TleDao;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class ScheduleTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Schedule schedule;
	private CelestrakServer celestrak;
	private LeoSatDataServerMock server;
	private SatnogsServerMock satnogs;
	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private Housekeeping houseKeeping;
	private long current;
	private ObservationFactory factory;

	@Test
	public void testScheduleForNewLaunches() throws Exception {
		satnogs.setSatellitesMock(new JsonHttpResponse("satnogs/satellites.json", 200));
		satnogs.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters.json", 200));
		satnogs.setTleMockDirectory("satnogs");
		server.setSatelliteMock(new JsonHttpResponse("r2cloudclienttest/satellite.json", 200));
		server.setNewLaunchMock(new JsonHttpResponse("r2cloudclienttest/newlaunch-for-scheduletest.json", 200));
		houseKeeping.run();
		current = getTime("2022-09-30 22:17:01.000");
		List<ObservationRequest> expected = readExpected("expected/scheduleNewLaunches.txt");
		List<ObservationRequest> actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), current);
		assertObservations(expected, actual);
	}

	@Test
	public void testSequentialTimetableForRotator() throws Exception {
		houseKeeping.run();
		schedule = new Schedule(new SequentialTimetable(Device.PARTIAL_TOLERANCE_MILLIS), factory);
		List<ObservationRequest> expected = readExpected("expected/schedule.txt");
		List<ObservationRequest> actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), current);
		assertObservations(expected, actual);
	}

	@Test
	public void testScheduleBasedOnOverlapedTimetable() throws Exception {
		houseKeeping.run();
		schedule = new Schedule(new OverlappedTimetable(Device.PARTIAL_TOLERANCE_MILLIS), factory);
		List<ObservationRequest> expected = readExpected("expected/scheduleOverlapedTimetable.txt");
		List<ObservationRequest> actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), current);
		assertObservations(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidObservationId() throws Exception {
		schedule.assignTasksToSlot(UUID.randomUUID().toString(), new ScheduledObservation(null, null, null, null));
	}

	@Test
	public void testEdgeCases() throws Exception {
		assertNull(schedule.cancel(null));
		assertNull(schedule.cancel(UUID.randomUUID().toString()));
	}

	@Test
	public void testBasicOperations() throws Exception {
		houseKeeping.run();
		List<ObservationRequest> expected = readExpected("expected/schedule.txt");
		List<ObservationRequest> actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), current);
		assertObservations(expected, actual);

		ObservationRequest first = schedule.findFirstByTransmitterId("40378-0", getTime("2020-10-01 11:38:34.491"));
		assertEquals("1601553418714-40378-0", first.getId());
		assertNull(schedule.findFirstByTransmitterId("40378-0", getTime("2020-10-02 11:43:56.801")));

		// tasks and ensure previous got cancelled
		ScheduledObservation tasks = new ScheduledObservation(null, null, null, null);
		schedule.assignTasksToSlot(first.getId(), tasks);
		schedule.assignTasksToSlot(first.getId(), tasks);
		assertFalse(tasks.isCancelled());
		ScheduledObservation differentTasks = new ScheduledObservation(null, null, null, null);
		schedule.assignTasksToSlot(first.getId(), differentTasks);
		assertTrue(tasks.isCancelled());

		List<ObservationRequest> sublist = schedule.findObservations(getTime("2020-10-01 10:55:40.000"), getTime("2020-10-01 13:04:14.000"));
		assertObservations(readExpected("expected/scheduleSublist.txt"), sublist);

		List<ObservationRequest> noaa18 = schedule.addToSchedule(satelliteDao.findByName("NOAA 18").getTransmitters().get(0), current);
		List<ObservationRequest> extended = new ArrayList<>(actual);
		extended.addAll(noaa18);
		assertObservations(readExpected("expected/scheduleWithNoaa18.txt"), extended);
		// test satellite already scheduled
		List<ObservationRequest> doubleAdded = schedule.addToSchedule(satelliteDao.findByName("NOAA 18").getTransmitters().get(0), current);
		assertObservations(noaa18, doubleAdded);

		// cancel all newly added
		for (ObservationRequest cur : noaa18) {
			ScheduledObservation curTasks = new ScheduledObservation(null, null, null, null);
			schedule.assignTasksToSlot(cur.getId(), curTasks);
			schedule.cancel(cur.getId());
			assertTrue(curTasks.isCancelled());
		}
		assertObservations(expected, actual);

		schedule.cancelAll();
		assertTrue(differentTasks.isCancelled());
		actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), current);
		assertObservations(expected, actual);

		first = schedule.findFirstByTransmitterId("40378-0", getTime("2020-10-01 11:38:34.491"));
		// slot is occupied
		assertNull(schedule.moveObservation(first, getTime("2020-10-02 11:11:00.359")));
		first = schedule.findFirstByTransmitterId("40378-0", getTime("2020-10-01 11:38:34.491"));
		ObservationRequest movedTo = schedule.moveObservation(first, getTime("2020-10-02 00:00:00.000"));
		assertNotNull(movedTo);
		assertEquals(getTime("2020-10-02 00:00:00.000"), movedTo.getStartTimeMillis());
		assertEquals(getTime("2020-10-02 00:05:37.617"), movedTo.getEndTimeMillis());

		schedule.cancelAll();
		long partialStart = getTime("2020-09-30 23:00:46.872");
		actual = schedule.createInitialSchedule(extractSatellites(expected, satelliteDao), partialStart);
		assertEquals(partialStart, actual.get(0).getStartTimeMillis());
	}

	@Before
	public void start() throws Exception {
		current = getTime("2020-09-30 22:17:01.000");

		celestrak = new CelestrakServer();
		celestrak.mockResponse(TestUtil.loadExpected("tle-2020-09-27.txt"));
		celestrak.start();
		server = new LeoSatDataServerMock();
		server.setSatelliteMock("[]", 200);
		server.setNewLaunchMock(new JsonHttpResponse("r2cloudclienttest/empty-array-response.json", 200));
		server.start();
		satnogs = new SatnogsServerMock();
		satnogs.setSatellitesMock("[]", 200);
		satnogs.setTransmittersMock("[]", 200);
		satnogs.start();
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("satellites.sdr", SdrType.RTLSDR.name().toLowerCase());
		config.setProperty("r2cloud.newLaunches", true);
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		config.setProperty("leosatdata.hostname", server.getUrl());
		config.setProperty("satnogs.satellites", true);
		config.setProperty("satnogs.hostname", satnogs.getUrl());
		config.setProperty("scheduler.elevation.min", 8);
		config.setProperty("scheduler.elevation.guaranteed", 20);
		config.setList("tle.urls", celestrak.getUrls());
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.txt").getAbsolutePath());
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.setProperty("satellites.satnogs.location", new File(tempFolder.getRoot(), "satnogs.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.location", new File(tempFolder.getRoot(), "leosatdata.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.new.location", new File(tempFolder.getRoot(), "leosatdata.new.json").getAbsolutePath());
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");
		LeoSatDataClient r2cloudClient = new LeoSatDataClient(config, new FixedClock(current));
		SatnogsClient satnogsClient = new SatnogsClient(config, new FixedClock(current));
		satelliteDao = new SatelliteDao(config);
		TleDao tleDao = new TleDao(config);
		houseKeeping = new Housekeeping(config, satelliteDao, new ThreadPoolFactoryImpl(60000), new CelestrakClient(config), tleDao, satnogsClient, r2cloudClient, null);
		PredictOreKit predict = new PredictOreKit(config);
		factory = new ObservationFactory(predict, config);
		schedule = new Schedule(new SequentialTimetable(Device.PARTIAL_TOLERANCE_MILLIS), factory);

	}

	@After
	public void stop() {
		if (celestrak != null) {
			celestrak.stop();
		}
		if (server != null) {
			server.stop();
		}
		if (satnogs != null) {
			satnogs.stop();
		}
		if (houseKeeping != null) {
			houseKeeping.stop();
		}
	}

	private static void assertObservations(List<ObservationRequest> expected, List<ObservationRequest> actual) {
		assertEquals(expected.size(), actual.size());
		Collections.sort(expected, ObservationRequestComparator.INSTANCE);
		Collections.sort(actual, ObservationRequestComparator.INSTANCE);
		for (int i = 0; i < expected.size(); i++) {
			assertObservation(expected.get(i), actual.get(i));
		}
	}

	private static void assertObservation(ObservationRequest expected, ObservationRequest actual) {
		assertEquals(expected.getSatelliteId(), actual.getSatelliteId());
		assertEquals(expected.getStartTimeMillis(), actual.getStartTimeMillis());
		assertEquals(expected.getEndTimeMillis(), actual.getEndTimeMillis());
	}

	private static List<Transmitter> extractSatellites(List<ObservationRequest> req, SatelliteDao dao) throws Exception {
		Set<String> ids = new HashSet<>();
		for (ObservationRequest cur : req) {
			ids.add(cur.getSatelliteId());
		}
		List<String> sorted = new ArrayList<>(ids);
		Collections.sort(sorted);
		List<Transmitter> result = new ArrayList<>();
		for (String cur : sorted) {
			Satellite curSatellite = dao.findById(cur);
			if (curSatellite == null) {
				continue;
			}
			result.addAll(curSatellite.getTransmitters());
		}
		return result;
	}

	private static List<ObservationRequest> readExpected(String filename) throws Exception {
		List<ObservationRequest> result = new ArrayList<>();
		SimpleDateFormat sdf = createDateFormatter();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(ScheduleTest.class.getClassLoader().getResourceAsStream(filename)))) {
			String curLine = null;
			Pattern COMMA = Pattern.compile(",");
			while ((curLine = r.readLine()) != null) {
				String[] parts = COMMA.split(curLine);
				ObservationRequest cur = new ObservationRequest();
				cur.setStartTimeMillis(sdf.parse(parts[0].trim()).getTime());
				cur.setEndTimeMillis(sdf.parse(parts[1].trim()).getTime());
				cur.setSatelliteId(parts[2].trim());
				result.add(cur);
			}
		}
		return result;
	}

	private static SimpleDateFormat createDateFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}

	private static long getTime(String str) throws Exception {
		return createDateFormatter().parse(str).getTime();
	}

	// used to create assertion .txt files
	@SuppressWarnings("unused")
	private void printObservations(List<ObservationRequest> actual) {
		SimpleDateFormat sdf = createDateFormatter();
		for (ObservationRequest cur : actual) {
			Satellite sat = satelliteDao.findById(cur.getSatelliteId());
			Transmitter transmitter = sat.getTransmitters().get(0);
			System.out.println(sdf.format(new Date(cur.getStartTimeMillis())) + ",  " + sdf.format(new Date(cur.getEndTimeMillis())) + ",\t\t" + cur.getSatelliteId() + "," + transmitter.getFrequencyBand().getCenter() + ", " + sat.getName());
		}
	}
}

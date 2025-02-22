package ru.r2cloud.simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.LogManager;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.FixedClock;
import ru.r2cloud.TestUtil;
import ru.r2cloud.device.Device;
import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.SequentialTimetable;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.Housekeeping;
import ru.r2cloud.tle.TleDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class UtilizationTest {

	public static void main(String[] args) throws Exception {
		LogManager.getLogManager().reset();
		File tleCache = new File(System.getProperty("java.io.tmpdir") + File.separator + "tle.json");
		if (tleCache.exists()) {
			tleCache.delete();
		}
		Configuration config;
		File userSettingsLocation = new File("target/.r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.setProperty("tle.cacheFileLocation", tleCache.getAbsolutePath()); // disble cache
		// test overlapped observations
//		config.setProperty("satellites.sdr", SdrType.SDRSERVER.name().toLowerCase());

		SimpleDateFormat sdf = createFormatter();
		long current = sdf.parse("2020-09-27 18:47:32").getTime();
		long start = sdf.parse("2020-09-27 18:47:32").getTime();
		long end = sdf.parse("2020-09-29 18:47:32").getTime(); // +2 days

		CelestrakServer celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(TestUtil.loadExpected("tle-2020-09-27.txt"));
		config.setList("tle.urls", celestrak.getUrls());
		PredictOreKit predict = new PredictOreKit(config);
		SatelliteDao satelliteDao = new SatelliteDao(config);
		TleDao tleDao = new TleDao(config);
		PriorityService priorityService = new PriorityService(config, new FixedClock(current));
		Housekeeping houseKeeping = new Housekeeping(config, satelliteDao, new ThreadPoolFactoryImpl(60000), new CelestrakClient(config, new FixedClock(current)), tleDao, null, null, null, priorityService, null);
		houseKeeping.start();
		ObservationFactory factory = new ObservationFactory(predict);

		List<Transmitter> enabledByDefault = getDefaultEnabled(satelliteDao);

		System.out.println("partial default: ");
		enabledByDefault = getDefaultEnabled(satelliteDao);
		while (!enabledByDefault.isEmpty()) {
			float utilization = calculatePartialUtilization(factory, enabledByDefault, start, end);
			System.out.println(enabledByDefault.size() + " " + utilization);
			enabledByDefault.remove(0);
		}
		System.out.println("partial 70cm: ");
		List<Transmitter> cm = loadFromFile(satelliteDao, "70cm-satellites.txt");
		while (!cm.isEmpty()) {
			float utilization = calculatePartialUtilization(factory, cm, start, end);
			System.out.println(cm.size() + " " + utilization);
			cm.remove(0);
		}
		celestrak.stop();
		houseKeeping.stop();
	}

	private static List<Transmitter> loadFromFile(SatelliteDao satelliteDao, String file) throws Exception {
		List<Transmitter> result = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(UtilizationTest.class.getClassLoader().getResourceAsStream(file)))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				result.addAll(satelliteDao.findByName(curLine.trim()).getTransmitters());
			}
		}
		return result;
	}

	private static float calculatePartialUtilization(ObservationFactory factory, List<Transmitter> satellites, long start, long end) {
		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.OMNIDIRECTIONAL);
		antenna.setMinElevation(8);
		antenna.setGuaranteedElevation(20);

		Schedule schedule = new Schedule(new SequentialTimetable(Device.PARTIAL_TOLERANCE_MILLIS), factory);
		List<ObservationRequest> happened = schedule.createInitialSchedule(antenna, satellites, start);

		long total = end - start;
		long utilized = 0;
		for (ObservationRequest cur : happened) {
			utilized += (cur.getEndTimeMillis() - cur.getStartTimeMillis());
		}
		return (utilized / (float) total);
	}

	private static SimpleDateFormat createFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}

	private static List<Transmitter> getDefaultEnabled(SatelliteDao dao) {
		List<Transmitter> result = new ArrayList<>();
		for (Satellite cur : dao.findEnabled()) {
			result.addAll(cur.getTransmitters());
		}
		return result;
	}

}

package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.device.Device;
import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;

public class UtilizationTest {

	public static void main(String[] args) throws Exception {
		Configuration config;
		File userSettingsLocation = new File("target/.r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");
		// test overlapped observations
//		config.setProperty("satellites.sdr", SdrType.SDRSERVER.name().toLowerCase());
//		config.setProperty("rotator.enabled", false);

		CelestrakServer celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(TestUtil.loadExpected("tle-2020-09-27.txt"));
		PredictOreKit predict = new PredictOreKit(config);
		SatelliteDao satelliteDao = new SatelliteDao(config, null);
		TLEDao tleDao = new TLEDao(config, satelliteDao, new CelestrakClient(celestrak.getUrls()));
		tleDao.start();
		ObservationFactory factory = new ObservationFactory(predict, tleDao, config);

		List<Transmitter> enabledByDefault = getDefaultEnabled(satelliteDao);

		System.out.println("partial default: ");
		enabledByDefault = getDefaultEnabled(satelliteDao);
		while (!enabledByDefault.isEmpty()) {
			float utilization = calculatePartialUtilization(factory, enabledByDefault);
			System.out.println(enabledByDefault.size() + " " + utilization);
			enabledByDefault.remove(0);
		}
		System.out.println("partial 70cm: ");
		List<Transmitter> cm = loadFromFile(satelliteDao, "70cm-satellites.txt");
		while (!cm.isEmpty()) {
			float utilization = calculatePartialUtilization(factory, cm);
			System.out.println(cm.size() + " " + utilization);
			cm.remove(0);
		}
		celestrak.stop();
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

	private static float calculatePartialUtilization(ObservationFactory factory, List<Transmitter> satellites) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		long start = sdf.parse("2020-09-27 11:13:00").getTime();
		long end = sdf.parse("2020-09-29 11:13:00").getTime(); // +2 days

		Schedule schedule = new Schedule(new SequentialTimetable(Device.PARTIAL_TOLERANCE_MILLIS), factory);
		List<ObservationRequest> happened = schedule.createInitialSchedule(satellites, start);

		long total = end - start;
		long utilized = 0;
		for (ObservationRequest cur : happened) {
			utilized += (cur.getEndTimeMillis() - cur.getStartTimeMillis());
		}
		return (utilized / (float) total);
	}

	private static List<Transmitter> getDefaultEnabled(SatelliteDao dao) {
		List<Transmitter> result = new ArrayList<>();
		for (Satellite cur : dao.findEnabled()) {
			// this satellite can't be visible on the tested ground station
			if (cur.getId().equals("44365") || cur.getId().equals("44832")) {
				continue;
			}
			result.addAll(cur.getTransmitters());
		}
		return result;
	}

}

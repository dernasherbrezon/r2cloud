package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;

public class UtilizationTest {

	public static void main(String[] args) throws Exception {
		Configuration config;
		File userSettingsLocation = new File("target/.r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), FileSystems.getDefault());
		}
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");

		CelestrakServer celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(TestUtil.loadExpected("tle-2020-09-27.txt"));
		PredictOreKit predict = new PredictOreKit(config);
		SatelliteDao satelliteDao = new SatelliteDao(config);
		TLEDao tleDao = new TLEDao(config, satelliteDao, new CelestrakClient(celestrak.getUrl()));
		tleDao.start();
		ObservationFactory factory = new ObservationFactory(predict, tleDao, config);

		List<Satellite> enabledByDefault = getDefaultEnabled(satelliteDao);

		System.out.println("default: ");
		while (!enabledByDefault.isEmpty()) {
			float utilization = calculateUtilization(satelliteDao, factory, enabledByDefault);
			System.out.println(enabledByDefault.size() + " " + utilization);
			enabledByDefault.remove(0);
		}
		System.out.println("70cm: ");
		List<Satellite> cm = loadFromFile(satelliteDao, "70cm-satellites.txt");

		calculatePercentTotal(satelliteDao, factory, cm);

		while (!cm.isEmpty()) {
			float utilization = calculateUtilization(satelliteDao, factory, cm);
			System.out.println(cm.size() + " " + utilization);
			cm.remove(0);
		}

		celestrak.stop();
	}

	private static List<Satellite> loadFromFile(SatelliteDao satelliteDao, String file) throws Exception {
		List<Satellite> result = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(UtilizationTest.class.getClassLoader().getResourceAsStream(file)))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				result.add(satelliteDao.findByName(curLine.trim()));
			}
		}
		return result;
	}

	private static void calculatePercentTotal(SatelliteDao satelliteDao, ObservationFactory factory, List<Satellite> satellites) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		long start = sdf.parse("2020-09-27 11:13:00").getTime();
		long end = start + 2 * 24 * 60 * 60 * 1000;
		for (int i = 0; i < 5; i++) {
			long total = 0;
			List<ObservationRequest> happened = calculateObservations(satelliteDao, factory, satellites, start, end);
			Map<Long, Long> totalBySatellite = new TreeMap<>();
			for (ObservationRequest req : happened) {
				long observationTime = req.getEndTimeMillis() - req.getStartTimeMillis();
				total += observationTime;

				Long prevSat = totalBySatellite.get(Long.valueOf(req.getSatelliteId()));
				if (prevSat == null) {
					prevSat = 0L;
				}
				prevSat += observationTime;
				totalBySatellite.put(Long.valueOf(req.getSatelliteId()), prevSat);
			}

			StringBuilder str = new StringBuilder();
			for (Entry<Long, Long> cur : totalBySatellite.entrySet()) {
				str.append(cur.getValue() / (float) total).append(" ");
			}
			System.out.println(str.toString().trim());

			start = end;
			end += 2 * 24 * 60 * 60 * 1000;
		}
	}

	private static float calculateUtilization(SatelliteDao satelliteDao, ObservationFactory factory, List<Satellite> satellites) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		long start = sdf.parse("2020-09-27 11:13:00").getTime();
		long end = sdf.parse("2020-09-29 11:13:00").getTime(); // +2 days

		List<ObservationRequest> happened = calculateObservations(satelliteDao, factory, satellites, start, end);

		long total = end - start;
		long utilized = 0;
		for (ObservationRequest cur : happened) {
			utilized += (cur.getEndTimeMillis() - cur.getStartTimeMillis());
		}
		return (utilized / (float) total);
	}

	private static List<ObservationRequest> calculateObservations(SatelliteDao satelliteDao, ObservationFactory factory, List<Satellite> satellites, long start, long end) {
		Schedule<ScheduledObservation> schedule = new Schedule<>();
		List<ObservationRequest> initialRequests = new ArrayList<>();
		for (Satellite cur : satellites) {
			ObservationRequest req = create(factory, schedule, start, cur, false);
			if (req == null) {
				continue;
			}
			initialRequests.add(req);
			schedule.add(new ScheduledObservation(req, null, null, null, null));
		}
		Collections.sort(initialRequests, ObservationRequestComparator.INSTANCE);
		List<ObservationRequest> happened = new ArrayList<>();
		while (!initialRequests.isEmpty()) {
			ObservationRequest cur = initialRequests.remove(0);
			happened.add(cur);

			ObservationRequest next = create(factory, schedule, cur.getEndTimeMillis(), satelliteDao.findById(cur.getSatelliteId()), false);
			if (next == null) {
				continue;
			}
			if (next.getStartTimeMillis() > end) {
				continue;
			}
			initialRequests.add(next);
			schedule.add(new ScheduledObservation(next, null, null, null, null));
			Collections.sort(initialRequests, ObservationRequestComparator.INSTANCE);
		}
		return happened;
	}

	private static List<Satellite> getDefaultEnabled(SatelliteDao dao) {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : dao.findAll()) {
			if (!cur.isEnabled()) {
				continue;
			}
			// this satellite can't be visible on the tested ground station
			if (cur.getId().equals("44365")) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	// copy from scheduler to simulate utilization
	private static ObservationRequest create(ObservationFactory factory, Schedule<ScheduledObservation> schedule, long current, Satellite cur, boolean immediately) {
		long next = current;
		while (!Thread.currentThread().isInterrupted()) {
			ObservationRequest observation = factory.create(new Date(next), cur, immediately);
			if (observation == null) {
				return null;
			}

			ScheduledObservation overlapped = schedule.getOverlap(observation.getStartTimeMillis(), observation.getEndTimeMillis());
			if (overlapped == null) {
				return observation;
			}

			if (immediately) {
				overlapped.cancel();
				return observation;
			}

			// find next
			next = observation.getEndTimeMillis();
		}
		return null;
	}
}

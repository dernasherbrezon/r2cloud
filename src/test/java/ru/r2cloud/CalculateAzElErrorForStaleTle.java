package ru.r2cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.TimeScalesFactory;

import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.util.Configuration;

public class CalculateAzElErrorForStaleTle {

	public static void main(String[] args) throws Exception {

		Configuration config = new Configuration(CalculateAzElErrorForStaleTle.class.getClassLoader().getResourceAsStream("config-dev.properties"), System.getProperty("user.home") + File.separator + ".r2cloud", "config-common-test.properties", FileSystems.getDefault());
		config.setProperty("locaiton.lat", "51.721");
		config.setProperty("locaiton.lon", "5.030");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");

		PredictOreKit predict = new PredictOreKit(config);
		TopocentricFrame groundStation = predict.getPosition(new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0));

		long reqStart = 1727055832679L;
		long reqEnd = 1727056477595L;

		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.UK);
		start.setTimeInMillis(reqStart);

		List<Integer> calculateForDays = new ArrayList<>();
		calculateForDays.add(-2);
		calculateForDays.add(-4);
		calculateForDays.add(-8);
		calculateForDays.add(-10);
		calculateForDays.add(-20);

		List<Position> theMostRecent = new ArrayList<>();

		Map<Integer, List<Position>> listOfCoordinates = new HashMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(CalculateAzElErrorForStaleTle.class.getClassLoader().getResourceAsStream("39444_historical.txt")))) {
			String line = null;
			while ((line = r.readLine()) != null) {
				org.orekit.propagation.analytical.tle.TLE tle = new org.orekit.propagation.analytical.tle.TLE(line, r.readLine());
				Date tleEpoch = tle.getDate().toDate(TimeScalesFactory.getUTC());
				int daysDiff = (int) ((tleEpoch.getTime() - reqStart) / (24 * 60 * 60 * 1000));
				if (!calculateForDays.contains(daysDiff) && daysDiff != -1) {
					// skip not interesting day
					continue;
				}
				System.out.println("processing TLE " + (-daysDiff) + " days ago: " + tleEpoch);
				calculateForDays.remove(Integer.valueOf(daysDiff));
				TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
				List<Position> current = new ArrayList<>();
				for (long curStart = reqStart; curStart < reqEnd; curStart += 1000) {
					current.add(predict.getSatellitePosition(curStart, groundStation, tlePropagator));
				}
				if (daysDiff == -1 && theMostRecent.isEmpty()) {
					theMostRecent.addAll(current);
				} else {
					listOfCoordinates.put(daysDiff, current);
				}
			}
		}

		System.out.println("saving the most recent trajectory");

		// not necessary. just for testing purposes
		try (BufferedWriter w = new BufferedWriter(new FileWriter("observation.csv"))) {
			w.append("az,el\n");
			for (Position cur : theMostRecent) {
				w.append(String.valueOf(cur.getAzimuth())).append(',').append(String.valueOf(cur.getElevation())).append('\n');
			}
		}

		System.out.println("calculating difference...");

		try (BufferedWriter w = new BufferedWriter(new FileWriter("diff.csv"))) {
			for (int j = 0; j < theMostRecent.size(); j++) {
				if (j == 0) {
					int i = 0;
					for (Entry<Integer, List<Position>> cur : listOfCoordinates.entrySet()) {
						String daysAgo = String.valueOf(Math.abs(cur.getKey()));
						if (i != 0) {
							w.append(',');
						}
						w.append("az").append(daysAgo).append(",el").append(daysAgo);
						i++;
					}
					w.append('\n');
				}
				int i = 0;
				for (Entry<Integer, List<Position>> cur : listOfCoordinates.entrySet()) {
					if (i != 0) {
						w.append(',');
					}
					w.append(String.valueOf(theMostRecent.get(j).getAzimuth() - cur.getValue().get(j).getAzimuth())).append(',').append(String.valueOf(theMostRecent.get(j).getElevation() - cur.getValue().get(j).getElevation()));
					i++;
				}
				w.append('\n');
			}
		}
	}

}

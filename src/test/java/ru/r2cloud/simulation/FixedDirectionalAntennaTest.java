package ru.r2cloud.simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.orekit.propagation.analytical.tle.TLEPropagator;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.FixedClock;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.util.Configuration;

public class FixedDirectionalAntennaTest {

	public static void main(String[] args) throws Exception {

		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.FIXED_DIRECTIONAL);
		antenna.setAzimuth(270);
		antenna.setElevation(30.0);
		antenna.setBeamwidth(45);
		// capture 4 minutes min
		long minimumObservationMillis = 4 * 60 * 1000;
		Configuration config;
		File userSettingsLocation = new File("target/.r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		CelestrakServer celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(TestUtil.loadExpected("tle-2020-09-27.txt"));
		config.setList("tle.urls", celestrak.getUrls());

		PredictOreKit predict = new PredictOreKit(config);

		SimpleDateFormat sdf = createFormatter();
		long current = sdf.parse("2020-09-27 18:47:32").getTime();

		CelestrakClient client = new CelestrakClient(config, new FixedClock(current));

		Map<String, Tle> tles = client.downloadTle();
		Set<String> supported = loadLeoSatellites("70cm-satellites.txt");

		int maxOutput = 50;
		int currentOutput = 0;

		JsonArray ds = new JsonArray();
		for (Tle cur : tles.values()) {
			if (!supported.contains(cur.getRaw()[0])) {
				continue;
			}
			TLEPropagator propagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(cur.getRaw()[1], cur.getRaw()[2]));
			List<SatPass> schedule = predict.calculateSchedule(antenna, new Date(current), propagator);
			for (SatPass curPass : schedule) {
				long length = curPass.getEndMillis() - curPass.getStartMillis();
				if (length < minimumObservationMillis) {
					continue;
				}
				ds.add(output(curPass, predict, propagator, currentOutput));

				currentOutput++;
				if (currentOutput >= maxOutput) {
					break;
				}
			}
			if (currentOutput >= maxOutput) {
				break;
			}
		}

		File output = new File("target" + File.separator + "fixedDirectional");
		if (!output.exists() && !output.mkdirs()) {
			System.out.println("can't create output directory");
			System.exit(1);
		}

		TestUtil.copy("fixedDirectional/index.html", new File(output, "index.html"));
		TestUtil.copy("fixedDirectional/AzElChart.js", new File(output, "AzElChart.js"));

		try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(output, "Data.js")))) {
			w.append("const dses = ").append(ds.toString(WriterConfig.PRETTY_PRINT)).append(";");
		}

		System.out.println("Results written to " + output.getAbsolutePath());

		System.exit(0);

	}

	private static JsonObject output(SatPass pass, PredictOreKit predict, TLEPropagator tle, int index) throws Exception {
		JsonArray points = new JsonArray();
		for (long i = pass.getStartMillis(); i < pass.getEndMillis(); i += 1000) {
			Position pos = predict.getSatellitePosition(i, predict.getPosition(), tle);
			JsonObject cur = new JsonObject();
			cur.add("x", pos.getAzimuth());
			cur.add("y", pos.getElevation());
			cur.add("time", i);
			points.add(cur);
		}
		JsonObject ds = new JsonObject();
		ds.add("label", "data " + index);
		ds.add("data", points);
		ds.add("borderColor", generateColor(Integer.hashCode((int) (index + pass.getStartMillis()))));
		ds.add("borderWidth", 1);
		return ds;
	}

	private static Set<String> loadLeoSatellites(String file) throws Exception {
		Set<String> result = new HashSet<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(FixedDirectionalAntennaTest.class.getClassLoader().getResourceAsStream(file)))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				result.add(curLine.trim());
			}
		}
		return result;
	}

	private static String generateColor(int hash) {
		StringBuilder result = new StringBuilder();
		result.append("#");
		for (int i = 0; i < 3; i++) {
			int cur = (hash >> (i * 8)) & 0xFF;
			String color = "00" + Integer.toHexString(cur);
			result.append(color.substring(color.length() - 2));
		}
		return result.toString();
	}

	private static SimpleDateFormat createFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}
}

package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.SatPass;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class PredictTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	
	@Test
	public void test() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "56.189713");
		config.setProperty("locaiton.lon", "38.17431");

		Satellite noaa15 = SatelliteFactory.createSatellite(new TLE(new String[] { "AAUSAT4", "1 41460U 16025E   18286.48591319  .00003726  00000-0  19222-3 0  9991", "2 41460  98.1207 356.1041 0161873 106.7501 255.1584 15.06318446135298" }));
		Predict s = new Predict(config);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = sdf.parse("14-10-2018 07:39:04");
		System.out.println(date.getTime());
		SatPass next = s.calculateNext(date, noaa15);
		assertNull(next);

	}

	// happens on initial startup
	@Test
	public void testPrediectWithoutBaseStationCoordinates() throws Exception {
		config.remove("locaiton.lat");
		config.remove("locaiton.lon");

		Satellite noaa15 = SatelliteFactory.createSatellite(new TLE(new String[] { "NOAA 15", "1 25338U 98030A   17271.51297398  .00000037  00000-0  34305-4 0  9992", "2 25338  98.7817 282.6269 0009465 266.6019  93.4077 14.25818111  7720" }));
		Predict s = new Predict(config);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		assertNull(s.calculateNext(sdf.parse("29-09-2017 14:54:00"), noaa15));
	}

	// expected pass times taken from wxtoimg
	@Test
	public void testSameAsWxToImg() throws Exception {
		Satellite noaa15 = SatelliteFactory.createSatellite(new TLE(new String[] { "NOAA 15", "1 25338U 98030A   17271.51297398  .00000037  00000-0  34305-4 0  9992", "2 25338  98.7817 282.6269 0009465 266.6019  93.4077 14.25818111  7720" }));
		Predict s = new Predict(config);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		assertPosition("18:05:58", "18:17:13", s.calculateNext(sdf.parse("29-09-2017 14:54:00"), noaa15));
		assertPosition("19:46:57", "19:56:34", s.calculateNext(sdf.parse("29-09-2017 19:00:00"), noaa15));
	}

	private static void assertPosition(String start, String end, SatPass pass) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		assertEquals(start, sdf.format(pass.getStart().getTime()));
		assertEquals(end, sdf.format(pass.getEnd().getTime()));
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
	}

}

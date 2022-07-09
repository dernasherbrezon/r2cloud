package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.propagation.analytical.tle.TLEPropagator;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.predict.PredictOreKit;

public class PredictOreKitTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private TLEPropagator noaa15;
	private PredictOreKit predict;

	@Test
	public void testMeoOrbit() throws Exception {
		TLEPropagator astroBio = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE("1 84002U          22194.52504630  .00000000  00000-0  00000-0 0    00", "2 84002  70.1600  50.0000 0000815  30.0000 147.0000  6.38669028    02"));
		assertPosition("16:00:57", "16:45:56", predict.calculateNext(getDate("29-09-2022 14:54:00"), astroBio));
	}

	@Test
	public void testCalculateBatch() throws Exception {
		List<SatPass> next2Days = predict.calculateSchedule(getDate("29-09-2017 14:54:00"), noaa15);
		assertPosition("18:05:57", "18:17:12", next2Days.get(0));
		assertEquals(8, next2Days.size());
	}

	// happens on initial startup
	@Test
	public void testPredictWithoutBaseStationCoordinates() throws Exception {
		config.remove("locaiton.lat");
		config.remove("locaiton.lon");

		assertNull(predict.calculateNext(getDate("29-09-2017 14:54:00"), noaa15));
	}

	// expected pass times taken from wxtoimg
	@Test
	public void testSameAsWxToImg() throws Exception {
		assertPosition("18:05:57", "18:17:12", predict.calculateNext(getDate("29-09-2017 14:54:00"), noaa15));
		assertPosition("19:46:56", "19:56:34", predict.calculateNext(getDate("29-09-2017 19:00:00"), noaa15));
	}

	private static Date getDate(String str) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		return sdf.parse(str);
	}

	private static void assertPosition(String start, String end, SatPass pass) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		assertEquals(start, sdf.format(new Date(pass.getStartMillis())));
		assertEquals(end, sdf.format(new Date(pass.getEndMillis())));
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		predict = new PredictOreKit(config);
		noaa15 = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE("1 25338U 98030A   17271.51297398  .00000037  00000-0  34305-4 0  9992", "2 25338  98.7817 282.6269 0009465 266.6019  93.4077 14.25818111  7720"));
	}

}

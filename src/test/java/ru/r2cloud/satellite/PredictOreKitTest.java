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
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.predict.PredictOreKit;

public class PredictOreKitTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private TLEPropagator noaa15;
	private PredictOreKit predict;
	private AntennaConfiguration antenna;

	@Test
	public void testMeoOrbit() throws Exception {
		TLEPropagator astroBio = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE("1 84002U          22194.52504630  .00000000  00000-0  00000-0 0    00", "2 84002  70.1600  50.0000 0000815  30.0000 147.0000  6.38669028    02"));
		assertPosition("16:00:57", "16:45:56", predict.calculateNext(antenna, getDate("29-09-2022 14:54:00"), astroBio));
	}

	@Test
	public void testCalculateBatch() throws Exception {
		List<SatPass> next2Days = predict.calculateSchedule(antenna, getDate("29-09-2017 14:54:00"), noaa15);
		assertPosition("18:05:57", "18:17:12", next2Days.get(0));
		assertEquals(8, next2Days.size());
	}

	// happens on initial startup
	@Test
	public void testPredictWithoutBaseStationCoordinates() throws Exception {
		config.remove("locaiton.lat");
		config.remove("locaiton.lon");

		assertNull(predict.calculateNext(antenna, getDate("29-09-2017 14:54:00"), noaa15));
	}

	// expected pass times taken from wxtoimg
	@Test
	public void testSameAsWxToImg() throws Exception {
		assertPosition("18:05:57", "18:17:12", predict.calculateNext(antenna, getDate("29-09-2017 14:54:00"), noaa15));
		assertPosition("19:46:56", "19:56:34", predict.calculateNext(antenna, getDate("29-09-2017 19:00:00"), noaa15));
	}

	@Test
	public void testFixedDirectionalAntenna() throws Exception {
		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.FIXED_DIRECTIONAL);
		antenna.setAzimuth(270);
		antenna.setElevation(0.0);
		antenna.setBeamwidth(45);
		TLEPropagator propagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE("1 43881U 18111F   24042.67399199  .00015427  00000+0  10192-2 0  9994", "2 43881  97.6384 310.9432 0012514 108.3312 251.9278 15.07433805280400"));
		assertPosition("11:29:31", "11:33:11", predict.calculateNext(antenna, getDate("12-02-2024 11:20:00"), propagator));
		assertPosition("11:30:00", "11:33:11", predict.calculateNext(antenna, getDate("12-02-2024 11:30:00"), propagator));
		List<SatPass> schedule = predict.calculateSchedule(antenna, getDate("12-02-2024 11:20:00"), propagator);
		assertEquals(4, schedule.size());
		assertPosition("11:29:31", "11:33:11", schedule.get(0));
		assertPosition("02:08:55", "02:12:45", schedule.get(1));
		assertPosition("12:58:34", "13:02:41", schedule.get(2));
		assertPosition("00:29:32", "00:31:55", schedule.get(3));
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
		antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.OMNIDIRECTIONAL);
		antenna.setMinElevation(8);
		antenna.setGuaranteedElevation(20);
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");

		predict = new PredictOreKit(config);
		noaa15 = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE("1 25338U 98030A   17271.51297398  .00000037  00000-0  34305-4 0  9992", "2 25338  98.7817 282.6269 0009465 266.6019  93.4077 14.25818111  7720"));
	}

}

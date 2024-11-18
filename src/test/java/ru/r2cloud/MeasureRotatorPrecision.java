package ru.r2cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.lora.loraat.JSerial;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.rotator.allview.AllviewClient;
import ru.r2cloud.rotator.allview.AllviewRotatorService;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.rotctrld.RotCtrlException;
import ru.r2cloud.rotctrld.RotctrldClient;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class MeasureRotatorPrecision {

	private static final Logger LOG = LoggerFactory.getLogger(MeasureRotatorPrecision.class);

	private static final int ROTCTRLD_PORT = 8000;
	private static final String ALLVIEW_DEVICE_FILE = "/dev/cu.usbserial-A10M67FQ";

	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration(MeasureRotatorPrecision.class.getClassLoader().getResourceAsStream("config-dev.properties"), System.getProperty("user.home") + File.separator + ".r2cloud", "config-common-test.properties", FileSystems.getDefault());
		config.setProperty("locaiton.lat", "51.721");
		config.setProperty("locaiton.lon", "5.030");
		config.setProperty("scheduler.orekit.path", "./src/test/resources/data/orekit-data");
		PredictOreKit predict = new PredictOreKit(config);

		Tle tle = new Tle(new String[] { "funcube-1", "1 39444U 13066AE  20157.75071106  .00000221  00000-0  33451-4 0  9997", "2 39444  97.5589 158.6491 0056696 309.6463  49.9756 14.82127945351637" });
		ObservationRequest req = new ObservationRequest();
		req.setTle(tle);
		req.setStartTimeMillis(getTime("2020-06-06 05:50:29"));
		req.setEndTimeMillis(getTime("2020-06-06 06:01:42"));
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		req.setGroundStation(new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0));

		OffsetClock clock = new OffsetClock(req.getStartTimeMillis());
		RotatorConfiguration rotatorConfig = createValidConfig();

		// choose which measure to take by uncommeting to the function
//		measureRotatorService(predict, req, clock, rotatorConfig);
//		measureAllviewService(predict, req, clock, rotatorConfig);
	}

	private static void measureAllviewService(PredictOreKit predict, ObservationRequest req, OffsetClock clock, RotatorConfiguration rotatorConfig) throws InterruptedException, IOException, FileNotFoundException {
		CountDownLatch latch = new CountDownLatch(1);
		final File result = new File("allview.csv");
		AllviewClient client = new AllviewClient(ALLVIEW_DEVICE_FILE, 5000, new JSerial());
		try {
			client.start();
		} catch (Exception e1) {
			LOG.error("unable to start allview client", e1);
			return;
		}
		Thread monitorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try (BufferedWriter w = new BufferedWriter(new FileWriter(result))) {
					while (!Thread.currentThread().isInterrupted()) {
						Position current = client.getPosition();
						long currentMillis = clock.millis();
						w.append(String.valueOf(current.getAzimuth())).append(",").append(String.valueOf(current.getElevation())).append(",").append(String.valueOf(currentMillis)).append("\n");
						if (currentMillis > req.getEndTimeMillis()) {
							break;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
					LOG.info("measurement complete. results at: {}", result.getAbsolutePath());
				} catch (IOException e) {
					LOG.error("cannot create file for metrics", e);
				} finally {
					latch.countDown();
				}
			}
		}, "monitor-thread");
		monitorThread.start();

		AllviewRotatorService service = new AllviewRotatorService(client, rotatorConfig, predict, new ThreadPoolFactoryImpl(10000), clock);
		service.start();
		Future<?> observationFuture = service.schedule(req, req.getStartTimeMillis(), null);
		latch.await();
		observationFuture.cancel(true);
		service.stop();

		enrichWithExpectedPosition(predict, req, result);
	}

	private static void measureRotatorService(PredictOreKit predict, ObservationRequest req, OffsetClock clock, RotatorConfiguration rotatorConfig) throws InterruptedException, IOException, FileNotFoundException {
		CountDownLatch latch = new CountDownLatch(1);
		final File result = new File("coarse.csv");
		Thread monitorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				RotctrldClient client = new RotctrldClient(rotatorConfig.getHostname(), rotatorConfig.getPort(), rotatorConfig.getTimeout());
				try (BufferedWriter w = new BufferedWriter(new FileWriter(result))) {
					client.start();
					while (!Thread.currentThread().isInterrupted()) {
						Position current;
						try {
							current = client.getPosition();
						} catch (RotCtrlException e1) {
							LOG.error("can't get position", e1);
							break;
						}
						long currentMillis = clock.millis();
						w.append(String.valueOf(current.getAzimuth())).append(",").append(String.valueOf(current.getElevation())).append(",").append(String.valueOf(currentMillis)).append("\n");
						if (currentMillis > req.getEndTimeMillis()) {
							break;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
					LOG.info("measurement complete. results at: {}", result.getAbsolutePath());
				} catch (IOException e) {
					LOG.error("cannot create file for metrics", e);
				} finally {
					latch.countDown();
				}
			}
		}, "monitor-thread");
		monitorThread.start();

		RotatorService service = new RotatorService(rotatorConfig, predict, new ThreadPoolFactoryImpl(10000), clock);
		service.start();
		Future<?> observationFuture = service.schedule(req, req.getStartTimeMillis(), null);
		latch.await();
		observationFuture.cancel(true);
		service.stop();

		enrichWithExpectedPosition(predict, req, result);
	}

	private static void enrichWithExpectedPosition(PredictOreKit predict, ObservationRequest req, final File result) throws IOException, FileNotFoundException {
		if (!result.exists()) {
			return;
		}
		try (BufferedReader r = new BufferedReader(new FileReader(result)); BufferedWriter w = new BufferedWriter(new FileWriter(result.getName() + "_enriched.csv"))) {
			String curLine = null;
			Pattern p = Pattern.compile(",");
			TopocentricFrame groundStation = predict.getPosition(req.getGroundStation());
			TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(req.getTle().getRaw()[1], req.getTle().getRaw()[2]));
			while ((curLine = r.readLine()) != null) {
				String[] parts = p.split(curLine);
				long time = Long.valueOf(parts[2]);
				Position satPosition = predict.getSatellitePosition(time, groundStation, tlePropagator);
				w.append(parts[0]).append(',').append(parts[1]).append(',').append(parts[2]).append(',').append(String.valueOf(satPosition.getAzimuth())).append(',').append(String.valueOf(satPosition.getElevation())).append('\n');
			}
		}
	}

	private static RotatorConfiguration createValidConfig() {
		RotatorConfiguration config = new RotatorConfiguration();
		config.setId(UUID.randomUUID().toString());
		config.setHostname("127.0.0.1");
		config.setPort(ROTCTRLD_PORT);
		config.setCycleMillis(1000);
		config.setTimeout(10000);
		config.setTolerance(5);
		return config;
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

}

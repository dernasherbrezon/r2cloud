package ru.r2cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.edu.icm.jlargearrays.ConcurrencyUtils;
import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraDevice;
import ru.r2cloud.device.SdrDevice;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.r2lora.ModulationConfig;
import ru.r2cloud.r2lora.R2loraClient;
import ru.r2cloud.r2lora.R2loraStatus;
import ru.r2cloud.satellite.LoraTransmitterFilter;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.SdrTransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.decoder.Decoders;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.tle.TLEReloader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ShutdownLoggingManager;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.ThreadPoolFactoryImpl;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.HttpContoller;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.AccessToken;
import ru.r2cloud.web.api.Health;
import ru.r2cloud.web.api.TLE;
import ru.r2cloud.web.api.configuration.Configured;
import ru.r2cloud.web.api.configuration.DDNS;
import ru.r2cloud.web.api.configuration.General;
import ru.r2cloud.web.api.configuration.PresentationMode;
import ru.r2cloud.web.api.configuration.R2CloudSave;
import ru.r2cloud.web.api.observation.ObservationList;
import ru.r2cloud.web.api.observation.ObservationLoad;
import ru.r2cloud.web.api.observation.ObservationLoadPresentation;
import ru.r2cloud.web.api.observation.ObservationSpectrogram;
import ru.r2cloud.web.api.schedule.ScheduleComplete;
import ru.r2cloud.web.api.schedule.ScheduleList;
import ru.r2cloud.web.api.schedule.ScheduleSave;
import ru.r2cloud.web.api.schedule.ScheduleStart;
import ru.r2cloud.web.api.setup.Restore;
import ru.r2cloud.web.api.setup.Setup;
import ru.r2cloud.web.api.status.MetricsController;
import ru.r2cloud.web.api.status.Overview;

public class R2Cloud {

	static {
		// must be called before any Logger method is used.
		System.setProperty("java.util.logging.manager", ShutdownLoggingManager.class.getName());
	}

	private static final Logger LOG = LoggerFactory.getLogger(R2Cloud.class);

	private static String version;

	private final Map<String, HttpContoller> controllers = new HashMap<>();
	private final WebServer webServer;
	private final Authenticator auth;
	private final Metrics metrics;
	private final AutoUpdate autoUpdate;
	private final DDNSClient ddnsClient;
	private final SatelliteDao satelliteDao;
	private final TLEDao tleDao;
	private final TLEReloader tleReloader;
	private final DecoderService decoderService;
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadFactory;
	private final ObservationFactory observationFactory;
	private final Clock clock;
	private final ProcessFactory processFactory;
	private final ObservationDao resultDao;
	private final LeoSatDataService leoSatDataService;
	private final LeoSatDataClient leoSatDataClient;
	private final SpectogramService spectogramService;
	private final Decoders decoders;
	private final SignedURL signed;
	private final DeviceManager deviceManager;

	public R2Cloud(Configuration props) {
		threadFactory = new ThreadPoolFactoryImpl(props.getThreadPoolShutdownMillis());
		processFactory = new ProcessFactory();
		clock = new DefaultClock();

		Integer numberOfThreads = props.getInteger("server.fft.threads");
		// if not specified, then number of available processors will be used
		if (numberOfThreads != null) {
			ConcurrencyUtils.setNumberOfThreads(numberOfThreads);
		}

		leoSatDataClient = new LeoSatDataClient(props);
		spectogramService = new SpectogramService(props);
		resultDao = new ObservationDao(props);
		leoSatDataService = new LeoSatDataService(props, resultDao, leoSatDataClient, spectogramService);
		metrics = new Metrics(props, clock);
		predict = new PredictOreKit(props);
		auth = new Authenticator(props);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		satelliteDao = new SatelliteDao(props, leoSatDataClient);
		tleDao = new TLEDao(props, satelliteDao, new CelestrakClient(props.getProperties("tle.urls")));
		tleReloader = new TLEReloader(props, tleDao, threadFactory, clock);
		signed = new SignedURL(props, clock);
		decoders = new Decoders(predict, props, processFactory, satelliteDao);
		decoderService = new DecoderService(props, decoders, resultDao, leoSatDataService, threadFactory, metrics, satelliteDao);

		observationFactory = new ObservationFactory(predict, tleDao, props);

		deviceManager = new DeviceManager(props, satelliteDao, threadFactory);
		for (DeviceConfiguration cur : props.getSdrConfigurations()) {
			int numberOfConcurrentObservations = 1;
			if (props.getSdrType().equals(SdrType.SDRSERVER) && cur.getRotatorConfiguration() == null) {
				numberOfConcurrentObservations = 5;
			}
			deviceManager.addDevice(new SdrDevice(cur.getId(), new SdrTransmitterFilter(cur), numberOfConcurrentObservations, observationFactory, threadFactory, clock, cur, resultDao, decoderService, predict, props, processFactory));
		}
		for (DeviceConfiguration cur : props.getLoraConfigurations()) {
			R2loraClient client = new R2loraClient(cur.getHostport(), cur.getUsername(), cur.getPassword(), cur.getTimeout());
			populateFrequencies(client.getStatus(), cur);
			deviceManager.addDevice(new LoraDevice(cur.getId(), new LoraTransmitterFilter(cur), 1, observationFactory, threadFactory, clock, cur, resultDao, decoderService, props, predict, client));
		}

		// setup web server
		index(new Health());
		index(new AccessToken(auth));
		index(new Setup(auth, props));
		index(new Configured(auth, props));
		index(new Restore(auth));
		index(new MetricsController(signed, metrics));
		index(new Overview(props, deviceManager));
		index(new General(props, autoUpdate));
		index(new DDNS(props, ddnsClient));
		index(new TLE(props, tleDao));
		index(new R2CloudSave(props));
		index(new ObservationSpectrogram(resultDao, spectogramService, signed));
		index(new ObservationList(satelliteDao, resultDao));
		index(new ObservationLoad(resultDao, signed, satelliteDao));
		index(new ObservationLoadPresentation(props, resultDao, signed, satelliteDao));
		index(new ScheduleList(satelliteDao, deviceManager));
		index(new ScheduleSave(satelliteDao, deviceManager));
		index(new ScheduleStart(satelliteDao, deviceManager));
		index(new ScheduleComplete(deviceManager));
		index(new PresentationMode(props, satelliteDao, resultDao, deviceManager));
		webServer = new WebServer(props, controllers, auth, signed);
	}

	public void start() {
		ddnsClient.start();
		tleDao.start();
		tleReloader.start();
		decoderService.start();
		// device manager should start after tle (it uses TLE to schedule
		// observations)
		deviceManager.start();
		metrics.start();
		webServer.start();
		LOG.info("=================================");
		LOG.info("=========== started =============");
		LOG.info("=================================");
	}

	public void stop() {
		webServer.stop();
		metrics.stop();
		deviceManager.stop();
		decoderService.stop();
		tleReloader.stop();
		tleDao.stop();
		ddnsClient.stop();
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			LOG.info("invalid arguments. expected: config.properties");
			return;
		}
		version = readVersion();
		R2Cloud app;
		String userPropertiesFilename = System.getProperty("user.home") + File.separator + ".r2cloud";
		try (InputStream is = new FileInputStream(args[0])) {
			Configuration props = new Configuration(is, userPropertiesFilename, FileSystems.getDefault());
			app = new R2Cloud(props);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					LOG.info("stopping");
					app.stop();
				} catch (Exception e) {
					LOG.error("unable to gracefully shutdown", e);
				} finally {
					LOG.info("=========== stopped =============");
					ShutdownLoggingManager.resetFinally();
				}
			}
		});
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				LOG.error("UncaughtException at: " + t.getName(), e);
			}
		});
		try {
			app.start();
		} catch (Exception e) {
			LOG.error("unable to start", e);
			// this will execute ShutdownHook and graceful shutdown
			System.exit(1);
		}
	}

	private static String readVersion() {
		InputStream is = null;
		try {
			is = R2Cloud.class.getClassLoader().getResourceAsStream("version.properties");
			if (is != null) {
				Properties props = new Properties();
				props.load(is);
				String result = props.getProperty("version", null);
				if (result != null) {
					return result;
				}
			}
		} catch (Exception e) {
			LOG.info("unable to read version. fallback to unknown. reason: {}", e.getMessage());
		} finally {
			Util.closeQuietly(is);
		}
		return null;
	}

	private void index(HttpContoller controller) {
		HttpContoller previous = controllers.put(controller.getRequestMappingURL(), controller);
		if (previous != null) {
			throw new IllegalArgumentException("duplicate controller has been registerd: " + controller.getClass().getSimpleName() + " previous: " + previous.getClass().getSimpleName());
		}
	}

	private static void populateFrequencies(R2loraStatus status, DeviceConfiguration config) {
		if (status.getConfigs() == null) {
			return;
		}
		for (ModulationConfig cur : status.getConfigs()) {
			if (!cur.getName().equalsIgnoreCase("lora")) {
				continue;
			}
			config.setMinimumFrequency((long) (cur.getMinFrequency() * 1_000_000));
			config.setMaximumFrequency((long) (cur.getMaxFrequency() * 1_000_000));
			return;
		}
	}

	public static String getVersion() {
		if (version == null) {
			return "unknown";
		}
		return version;
	}
}
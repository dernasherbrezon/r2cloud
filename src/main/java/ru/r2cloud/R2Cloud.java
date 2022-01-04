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
import ru.r2cloud.cloud.R2ServerClient;
import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.jradio.ax25.Ax25Beacon;
import ru.r2cloud.jradio.csp.CspBeacon;
import ru.r2cloud.jradio.fox.Fox1BBeacon;
import ru.r2cloud.jradio.fox.Fox1CBeacon;
import ru.r2cloud.jradio.fox.Fox1DBeacon;
import ru.r2cloud.jradio.meznsat.MeznsatBeacon;
import ru.r2cloud.jradio.usp.UspBeacon;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.satellite.decoder.APTDecoder;
import ru.r2cloud.satellite.decoder.Aausat4Decoder;
import ru.r2cloud.satellite.decoder.AfskAx25Decoder;
import ru.r2cloud.satellite.decoder.Alsat1nDecoder;
import ru.r2cloud.satellite.decoder.Ao73Decoder;
import ru.r2cloud.satellite.decoder.AstrocastDecoder;
import ru.r2cloud.satellite.decoder.BpskAx25Decoder;
import ru.r2cloud.satellite.decoder.BpskAx25G3ruhDecoder;
import ru.r2cloud.satellite.decoder.ChompttDecoder;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.decoder.DelfiC3Decoder;
import ru.r2cloud.satellite.decoder.Diy1Decoder;
import ru.r2cloud.satellite.decoder.Dstar1Decoder;
import ru.r2cloud.satellite.decoder.EseoDecoder;
import ru.r2cloud.satellite.decoder.Floripasat1Decoder;
import ru.r2cloud.satellite.decoder.FoxDecoder;
import ru.r2cloud.satellite.decoder.FoxSlowDecoder;
import ru.r2cloud.satellite.decoder.FskAx100Decoder;
import ru.r2cloud.satellite.decoder.FskAx25G3ruhDecoder;
import ru.r2cloud.satellite.decoder.Gomx1Decoder;
import ru.r2cloud.satellite.decoder.Huskysat1Decoder;
import ru.r2cloud.satellite.decoder.ItSpinsDecoder;
import ru.r2cloud.satellite.decoder.Itasat1Decoder;
import ru.r2cloud.satellite.decoder.Jy1satDecoder;
import ru.r2cloud.satellite.decoder.LRPTDecoder;
import ru.r2cloud.satellite.decoder.Lucky7Decoder;
import ru.r2cloud.satellite.decoder.Nayif1Decoder;
import ru.r2cloud.satellite.decoder.OpsSatDecoder;
import ru.r2cloud.satellite.decoder.PegasusDecoder;
import ru.r2cloud.satellite.decoder.ReaktorHelloWorldDecoder;
import ru.r2cloud.satellite.decoder.SalsatDecoder;
import ru.r2cloud.satellite.decoder.SnetDecoder;
import ru.r2cloud.satellite.decoder.Strand1Decoder;
import ru.r2cloud.satellite.decoder.Suomi100Decoder;
import ru.r2cloud.satellite.decoder.TechnosatDecoder;
import ru.r2cloud.satellite.decoder.UspDecoder;
import ru.r2cloud.sdr.SdrLock;
import ru.r2cloud.sdr.SdrStatusDao;
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
import ru.r2cloud.web.api.configuration.R2CloudSave;
import ru.r2cloud.web.api.observation.ObservationList;
import ru.r2cloud.web.api.observation.ObservationLoad;
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
	private final SdrStatusDao rtlsdrStatusDao;
	private final AutoUpdate autoUpdate;
	private final DDNSClient ddnsClient;
	private final SatelliteDao satelliteDao;
	private final TLEDao tleDao;
	private final TLEReloader tleReloader;
	private final DecoderService decoderService;
	private final Schedule schedule;
	private final Scheduler scheduler;
	private final SdrLock rtlsdrLock;
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadFactory;
	private final ObservationFactory observationFactory;
	private final Clock clock;
	private final ProcessFactory processFactory;
	private final ObservationDao resultDao;
	private final R2ServerService r2cloudService;
	private final R2ServerClient r2cloudClient;
	private final SpectogramService spectogramService;
	private final Map<String, Decoder> decoders = new HashMap<>();
	private final SignedURL signed;
	private final RotatorService rotatorService;

	public R2Cloud(Configuration props) {
		threadFactory = new ThreadPoolFactoryImpl();
		processFactory = new ProcessFactory();
		clock = new DefaultClock();

		rtlsdrLock = new SdrLock();
		rtlsdrLock.register(Scheduler.class, 3);
		rtlsdrLock.register(SdrStatusDao.class, 2);

		Integer numberOfThreads = props.getInteger("server.fft.threads");
		// if not specified, then number of available processors will be used
		if (numberOfThreads != null) {
			ConcurrencyUtils.setNumberOfThreads(numberOfThreads);
		}

		r2cloudClient = new R2ServerClient(props);
		spectogramService = new SpectogramService(props);
		resultDao = new ObservationDao(props);
		r2cloudService = new R2ServerService(props, resultDao, r2cloudClient, spectogramService);
		metrics = new Metrics(props, clock);
		predict = new PredictOreKit(props);
		auth = new Authenticator(props);
		rtlsdrStatusDao = new SdrStatusDao(props, rtlsdrLock, threadFactory, metrics, processFactory);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		satelliteDao = new SatelliteDao(props, r2cloudClient);
		tleDao = new TLEDao(props, satelliteDao, new CelestrakClient(props.getProperty("celestrak.hostname"), props.getProperty("calpoly.hostname")));
		tleReloader = new TLEReloader(props, tleDao, threadFactory, clock);
		signed = new SignedURL(props, clock);
		rotatorService = new RotatorService(props, predict, threadFactory, clock, metrics);
		APTDecoder aptDecoder = new APTDecoder(props, processFactory);
		decoders.put("25338", aptDecoder);
		decoders.put("28654", aptDecoder);
		decoders.put("32789", new DelfiC3Decoder(predict, props));
		decoders.put("33591", aptDecoder);
		decoders.put("39430", new Gomx1Decoder(predict, props));
		decoders.put("39444", new Ao73Decoder(predict, props));
		decoders.put("40069", new LRPTDecoder(predict, props));
		decoders.put("41460", new Aausat4Decoder(predict, props));
		decoders.put("42017", new Nayif1Decoder(predict, props));
		decoders.put("42784", new PegasusDecoder(predict, props));
		decoders.put("42829", new TechnosatDecoder(predict, props));
		decoders.put("48900", new TechnosatDecoder(predict, props));
		SnetDecoder snetDecoder = new SnetDecoder(predict, props);
		decoders.put("43186", snetDecoder);
		decoders.put("43187", snetDecoder);
		decoders.put("43188", snetDecoder);
		decoders.put("43189", snetDecoder);
		decoders.put("43743", new ReaktorHelloWorldDecoder(predict, props));
		decoders.put("43786", new Itasat1Decoder(predict, props));
		decoders.put("43792", new EseoDecoder(predict, props));
		decoders.put("43798", new AstrocastDecoder(predict, props));
		decoders.put("44083", new AstrocastDecoder(predict, props));
		decoders.put("43803", new Jy1satDecoder(predict, props));
		decoders.put("43804", new Suomi100Decoder(predict, props));
		decoders.put("43881", new Dstar1Decoder(predict, props));
		decoders.put("44406", new Lucky7Decoder(predict, props));
		decoders.put("44878", new OpsSatDecoder(predict, props));
		decoders.put("44885", new Floripasat1Decoder(predict, props));
		decoders.put("43017", new FoxSlowDecoder<>(predict, props, Fox1BBeacon.class));
		decoders.put("43770", new FoxSlowDecoder<>(predict, props, Fox1CBeacon.class));
		decoders.put("43137", new FoxDecoder<>(predict, props, Fox1DBeacon.class));
		decoders.put("45119", new Huskysat1Decoder(predict, props));
		decoders.put("43855", new ChompttDecoder(predict, props));
		decoders.put("41789", new Alsat1nDecoder(predict, props));
		decoders.put("39090", new Strand1Decoder(predict, props));
		decoders.put("46495", new SalsatDecoder(predict, props));
		decoders.put("46489", new BpskAx25G3ruhDecoder(predict, props, 2400, 1200, MeznsatBeacon.class));
		decoders.put("42792", new AfskAx25Decoder(predict, props, 1200, 1300, Ax25Beacon.class));
		decoders.put("39428", new BpskAx25Decoder(predict, props, 2400, 1200, Ax25Beacon.class));
		decoders.put("42790", new Gomx1Decoder(predict, props, CspBeacon.class, false, true, true));
		decoders.put("49017", new ItSpinsDecoder(predict, props, Ax25Beacon.class));
		decoders.put("47960", new UspDecoder(predict, props, 2400, UspBeacon.class));
		decoders.put("47952", new UspDecoder(predict, props, 2400, UspBeacon.class));
		decoders.put("47951", new UspDecoder(predict, props, 2400, UspBeacon.class));
		decoders.put("47963", new Diy1Decoder(predict, props));

		for (Satellite cur : satelliteDao.findAll()) {
			if (cur.getFraming() == null || cur.getModulation() == null || cur.getBeaconClass() == null || cur.getBaudRates() == null || cur.getBaudRates().isEmpty()) {
				continue;
			}
			if (cur.getModulation().equals(Modulation.GFSK) && cur.getFraming().equals(Framing.AX25G3RUH)) {
				decoders.put(cur.getId(), new FskAx25G3ruhDecoder(predict, props, cur.getBaudRates().get(0), cur.getBeaconClass()));
			} else if (cur.getModulation().equals(Modulation.GFSK) && cur.getFraming().equals(Framing.AX100)) {
				if (cur.getBeaconSizeBytes() == 0) {
					LOG.error("beacon size bytes are missing for GFSK AX100: {}", cur.getId());
					continue;
				}
				decoders.put(cur.getId(), new FskAx100Decoder(predict, props, cur.getBeaconSizeBytes(), cur.getBeaconClass(), cur.getBaudRatesAsArray()));
			} else if (cur.getModulation().equals(Modulation.BPSK) && cur.getFraming().equals(Framing.AX25G3RUH)) {
				decoders.put(cur.getId(), new BpskAx25G3ruhDecoder(predict, props, cur.getBaudRates().get(0), cur.getBeaconClass()));
			} else if (cur.getModulation().equals(Modulation.BPSK) && cur.getFraming().equals(Framing.AX25)) {
				decoders.put(cur.getId(), new BpskAx25Decoder(predict, props, cur.getBaudRates().get(0), cur.getBeaconClass()));
			} else if (cur.getModulation().equals(Modulation.AFSK) && cur.getFraming().equals(Framing.AX25)) {
				decoders.put(cur.getId(), new AfskAx25Decoder(predict, props, cur.getBaudRates().get(0), cur.getBeaconClass()));
			} else {
				LOG.error("unsupported combination of modulation and framing: {} - {}", cur.getModulation(), cur.getFraming());
			}
		}

		validateDecoders();
		decoderService = new DecoderService(props, decoders, resultDao, r2cloudService, threadFactory, metrics);

		observationFactory = new ObservationFactory(predict, tleDao, props);
		schedule = new Schedule(props, observationFactory);
		scheduler = new Scheduler(schedule, props, satelliteDao, rtlsdrLock, threadFactory, clock, processFactory, resultDao, decoderService, rotatorService);

		// setup web server
		index(new Health());
		index(new AccessToken(auth));
		index(new Setup(auth, props));
		index(new Configured(auth, props));
		index(new Restore(auth));
		index(new MetricsController(signed, metrics));
		index(new Overview(metrics));
		index(new General(props, autoUpdate));
		index(new DDNS(props, ddnsClient));
		index(new TLE(props, tleDao));
		index(new R2CloudSave(props));
		index(new ObservationSpectrogram(resultDao, spectogramService, signed));
		index(new ObservationList(satelliteDao, resultDao));
		index(new ObservationLoad(resultDao, signed, decoders));
		index(new ScheduleList(satelliteDao, schedule));
		index(new ScheduleSave(satelliteDao, scheduler));
		index(new ScheduleStart(satelliteDao, scheduler));
		index(new ScheduleComplete(scheduler));
		webServer = new WebServer(props, controllers, auth, signed);
	}

	public void start() {
		ddnsClient.start();
		rtlsdrStatusDao.start();
		tleDao.start();
		tleReloader.start();
		decoderService.start();
		rotatorService.start();
		// scheduler should start after tle (it uses TLE to schedule
		// observations)
		scheduler.start();
		metrics.start();
		webServer.start();
		LOG.info("=================================");
		LOG.info("=========== started =============");
		LOG.info("=================================");
	}

	public void stop() {
		webServer.stop();
		metrics.stop();
		scheduler.stop();
		rotatorService.stop();
		decoderService.stop();
		tleReloader.stop();
		tleDao.stop();
		rtlsdrStatusDao.stop();
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

	private void validateDecoders() {
		for (Satellite cur : satelliteDao.findAll()) {
			if (!decoders.containsKey(cur.getId())) {
				throw new IllegalStateException("decoder is not defined for: " + cur.getId());
			}
		}
		for (String id : decoders.keySet()) {
			if (satelliteDao.findById(id) == null) {
				throw new IllegalStateException("missing satellite configuration for: " + id);
			}
		}
	}

	public static String getVersion() {
		if (version == null) {
			return "unknown";
		}
		return version;
	}
}
package ru.r2cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2CloudClient;
import ru.r2cloud.cloud.R2CloudService;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.rx.ADSB;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.satellite.decoder.APTDecoder;
import ru.r2cloud.satellite.decoder.Aausat4Decoder;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.decoder.KunsPfDecoder;
import ru.r2cloud.satellite.decoder.LRPTDecoder;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.tle.CelestrakClient;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.tle.TLEReloader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ShutdownLoggingManager;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.ThreadPoolFactoryImpl;
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
import ru.r2cloud.web.api.configuration.SSL;
import ru.r2cloud.web.api.configuration.SSLLog;
import ru.r2cloud.web.api.observation.ObservationList;
import ru.r2cloud.web.api.observation.ObservationLoad;
import ru.r2cloud.web.api.observation.ObservationSpectrogram;
import ru.r2cloud.web.api.schedule.ScheduleList;
import ru.r2cloud.web.api.schedule.ScheduleSave;
import ru.r2cloud.web.api.setup.Restore;
import ru.r2cloud.web.api.setup.Setup;
import ru.r2cloud.web.api.status.Overview;

public class R2Cloud {

	static {
		// must be called before any Logger method is used.
		System.setProperty("java.util.logging.manager", ShutdownLoggingManager.class.getName());
	}

	private static final Logger LOG = LoggerFactory.getLogger(R2Cloud.class);

	private final Map<String, HttpContoller> controllers = new HashMap<>();
	private final WebServer webServer;
	private final ADSB adsb;
	private final ADSBDao dao;
	private final Authenticator auth;
	private final Metrics metrics;
	private final RtlSdrStatusDao rtlsdrStatusDao;
	private final AutoUpdate autoUpdate;
	private final DDNSClient ddnsClient;
	private final AcmeClient acmeClient;
	private final SatelliteDao satelliteDao;
	private final TLEDao tleDao;
	private final TLEReloader tleReloader;
	private final Scheduler scheduler;
	private final RtlSdrLock rtlsdrLock;
	private final Predict predict;
	private final ThreadPoolFactory threadFactory;
	private final ObservationFactory observationFactory;
	private final Clock clock;
	private final ProcessFactory processFactory;
	private final ObservationResultDao resultDao;
	private final R2CloudService r2cloudService;
	private final R2CloudClient r2cloudClient;
	private final SpectogramService spectogramService;
	private final Map<String, Decoder> decoders = new HashMap<>();

	public R2Cloud(InputStream systemProperties, String userProperties) throws IOException {
		Configuration props = new Configuration(systemProperties, userProperties);
		threadFactory = new ThreadPoolFactoryImpl();
		processFactory = new ProcessFactory();
		clock = new DefaultClock();

		rtlsdrLock = new RtlSdrLock();
		rtlsdrLock.register(Scheduler.class, 3);
		rtlsdrLock.register(RtlSdrStatusDao.class, 2);
		rtlsdrLock.register(ADSB.class, 1);

		r2cloudClient = new R2CloudClient(props);
		spectogramService = new SpectogramService(props);
		resultDao = new ObservationResultDao(props);
		r2cloudService = new R2CloudService(props, resultDao, r2cloudClient, spectogramService);
		predict = new Predict(props);
		dao = new ADSBDao(props);
		adsb = new ADSB(props, dao, rtlsdrLock, processFactory);
		auth = new Authenticator(props);
		metrics = new Metrics(props, r2cloudService);
		rtlsdrStatusDao = new RtlSdrStatusDao(props, rtlsdrLock);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		acmeClient = new AcmeClient(props);
		satelliteDao = new SatelliteDao(props);
		tleDao = new TLEDao(props, satelliteDao, new CelestrakClient("http://celestrak.com"));
		tleReloader = new TLEReloader(props, tleDao, threadFactory, clock);

		APTDecoder aptDecoder = new APTDecoder(props, processFactory);
		decoders.put("25338", aptDecoder);
		decoders.put("28654", aptDecoder);
		decoders.put("33591", aptDecoder);
		decoders.put("40069", new LRPTDecoder(props, predict));
		decoders.put("41460", new Aausat4Decoder(props, predict));
		decoders.put("43466", new KunsPfDecoder(props, predict));
		validateDecoders();

		observationFactory = new ObservationFactory(predict, tleDao);
		scheduler = new Scheduler(props, satelliteDao, rtlsdrLock, observationFactory, threadFactory, clock, r2cloudService, processFactory, resultDao, decoders);

		// setup web server
		index(new Health());
		index(new ru.r2cloud.web.api.ADSB(dao));
		index(new AccessToken(auth));
		index(new Setup(auth, props));
		index(new Configured(auth, props));
		index(new Restore(auth));
		index(new ru.r2cloud.web.api.status.Metrics());
		index(new Overview());
		index(new General(props, autoUpdate));
		index(new DDNS(props, ddnsClient));
		index(new SSL(props, acmeClient));
		index(new SSLLog(acmeClient));
		index(new TLE(props, tleDao));
		index(new R2CloudSave(props));
		index(new ObservationSpectrogram(resultDao, spectogramService));
		index(new ObservationList(satelliteDao, resultDao));
		index(new ObservationLoad(resultDao));
		index(new ScheduleList(satelliteDao, scheduler));
		index(new ScheduleSave(satelliteDao, scheduler));
		webServer = new WebServer(props, controllers, auth);
	}

	public void start() {
		metrics.start();
		dao.start();
		adsb.start();
		acmeClient.start();
		ddnsClient.start();
		rtlsdrStatusDao.start();
		tleDao.start();
		tleReloader.start();
		// scheduler should start after tle (it uses TLE to schedule
		// observations)
		scheduler.start();
		webServer.start();
		LOG.info("=================================");
		LOG.info("=========== started =============");
		LOG.info("=================================");
	}

	public void stop() {
		webServer.stop();
		scheduler.stop();
		tleReloader.stop();
		tleDao.stop();
		rtlsdrStatusDao.stop();
		ddnsClient.stop();
		acmeClient.stop();
		adsb.stop();
		dao.stop();
		metrics.stop();
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			LOG.info("invalid arguments. expected: config.properties");
			return;
		}
		R2Cloud app;
		String userPropertiesFilename = System.getProperty("user.home") + File.separator + ".r2cloud";
		try (InputStream is = new FileInputStream(args[0])) {
			app = new R2Cloud(is, userPropertiesFilename);
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
		app.start();
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
	}
}
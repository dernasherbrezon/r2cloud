package ru.r2cloud;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.rx.ADSB;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
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
import ru.r2cloud.web.api.configuration.General;
import ru.r2cloud.web.api.status.Overview;
import ru.r2cloud.web.controller.ADSBData;
import ru.r2cloud.web.controller.DoRestore;
import ru.r2cloud.web.controller.DoSetup;
import ru.r2cloud.web.controller.GetAcmeLog;
import ru.r2cloud.web.controller.LoadTLE;
import ru.r2cloud.web.controller.Restore;
import ru.r2cloud.web.controller.SaveConfiguration;
import ru.r2cloud.web.controller.Setup;
import ru.r2cloud.web.controller.weather.EnableWeather;
import ru.r2cloud.web.controller.weather.LoadWeatherSatellites;

public class R2Cloud {

	static {
		// must be called before any Logger method is used.
		System.setProperty("java.util.logging.manager", ShutdownLoggingManager.class.getName());
	}

	private static final Logger LOG = LoggerFactory.getLogger(R2Cloud.class);

	private final Configuration props;

	private final Map<String, HttpContoller> controllers = new HashMap<String, HttpContoller>();
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

	public R2Cloud(String propertiesLocation) {
		props = new Configuration(propertiesLocation, System.getProperty("user.home") + File.separator + ".r2cloud");
		threadFactory = new ThreadPoolFactoryImpl();
		processFactory = new ProcessFactory();
		clock = new DefaultClock();

		rtlsdrLock = new RtlSdrLock();
		rtlsdrLock.register(Scheduler.class, 3);
		rtlsdrLock.register(RtlSdrStatusDao.class, 2);
		rtlsdrLock.register(ADSB.class, 1);

		predict = new Predict(props);
		dao = new ADSBDao(props);
		adsb = new ADSB(props, dao, rtlsdrLock);
		auth = new Authenticator(props);
		metrics = new Metrics(props);
		rtlsdrStatusDao = new RtlSdrStatusDao(props, rtlsdrLock);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		acmeClient = new AcmeClient(props);
		satelliteDao = new SatelliteDao(props);
		tleDao = new TLEDao(props, satelliteDao, new CelestrakClient("http://celestrak.com"));
		tleReloader = new TLEReloader(props, tleDao, threadFactory, clock);
		observationFactory = new ObservationFactory(props, predict, tleDao, processFactory);
		scheduler = new Scheduler(props, satelliteDao, rtlsdrLock, observationFactory, threadFactory, clock);

		// setup web server
		index(new ru.r2cloud.web.controller.ADSB(props));
		index(new ADSBData(dao));
		index(new AccessToken(auth));
		index(new Setup());
		index(new DoSetup(auth, props));
		index(new Restore());
		index(new DoRestore(auth));
		index(new ru.r2cloud.web.api.status.Metrics());
		index(new Overview());
		index(new General(props, autoUpdate));
		index(new ru.r2cloud.web.controller.Configuration(props, autoUpdate, acmeClient));
		index(new SaveConfiguration(props, autoUpdate, ddnsClient, acmeClient));
		index(new GetAcmeLog(acmeClient));
		index(new LoadTLE(props, tleDao));
		index(new LoadWeatherSatellites(props, satelliteDao, scheduler));
		index(new EnableWeather(props));
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
		R2Cloud app = new R2Cloud(args[0]);
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
}
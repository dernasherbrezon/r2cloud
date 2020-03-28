package ru.r2cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2ServerClient;
import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.satellite.decoder.APTDecoder;
import ru.r2cloud.satellite.decoder.Aausat4Decoder;
import ru.r2cloud.satellite.decoder.Aistechsat3Decoder;
import ru.r2cloud.satellite.decoder.Ao73Decoder;
import ru.r2cloud.satellite.decoder.AstrocastDecoder;
import ru.r2cloud.satellite.decoder.Atl1Decoder;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.decoder.DecoderTask;
import ru.r2cloud.satellite.decoder.Dstar1Decoder;
import ru.r2cloud.satellite.decoder.EseoDecoder;
import ru.r2cloud.satellite.decoder.Floripasat1Decoder;
import ru.r2cloud.satellite.decoder.Gomx1Decoder;
import ru.r2cloud.satellite.decoder.Itasat1Decoder;
import ru.r2cloud.satellite.decoder.Jy1satDecoder;
import ru.r2cloud.satellite.decoder.KunsPfDecoder;
import ru.r2cloud.satellite.decoder.LRPTDecoder;
import ru.r2cloud.satellite.decoder.Lucky7Decoder;
import ru.r2cloud.satellite.decoder.Lume1Decoder;
import ru.r2cloud.satellite.decoder.Mysat1Decoder;
import ru.r2cloud.satellite.decoder.Nayif1Decoder;
import ru.r2cloud.satellite.decoder.OpsSatDecoder;
import ru.r2cloud.satellite.decoder.PegasusDecoder;
import ru.r2cloud.satellite.decoder.PwSat2Decoder;
import ru.r2cloud.satellite.decoder.ReaktorHelloWorldDecoder;
import ru.r2cloud.satellite.decoder.SmogPDecoder;
import ru.r2cloud.satellite.decoder.SnetDecoder;
import ru.r2cloud.satellite.decoder.Suomi100Decoder;
import ru.r2cloud.satellite.decoder.TechnosatDecoder;
import ru.r2cloud.ssl.AcmeClient;
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

	private final Map<String, HttpContoller> controllers = new HashMap<>();
	private final WebServer webServer;
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
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadFactory;
	private final ObservationFactory observationFactory;
	private final Clock clock;
	private final ProcessFactory processFactory;
	private final ObservationResultDao resultDao;
	private final R2ServerService r2cloudService;
	private final R2ServerClient r2cloudClient;
	private final SpectogramService spectogramService;
	private final Map<String, Decoder> decoders = new HashMap<>();
	private final SignedURL signed;

	public R2Cloud(Configuration props) {
		threadFactory = new ThreadPoolFactoryImpl();
		processFactory = new ProcessFactory();
		clock = new DefaultClock();

		rtlsdrLock = new RtlSdrLock();
		rtlsdrLock.register(Scheduler.class, 3);
		rtlsdrLock.register(RtlSdrStatusDao.class, 2);

		r2cloudClient = new R2ServerClient(props);
		spectogramService = new SpectogramService(props);
		resultDao = new ObservationResultDao(props);
		r2cloudService = new R2ServerService(props, resultDao, r2cloudClient, spectogramService);
		metrics = new Metrics(props, r2cloudService, clock);
		predict = new PredictOreKit(props);
		auth = new Authenticator(props);
		rtlsdrStatusDao = new RtlSdrStatusDao(props, rtlsdrLock, threadFactory, metrics, processFactory);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		acmeClient = new AcmeClient(props);
		satelliteDao = new SatelliteDao(props);
		tleDao = new TLEDao(props, satelliteDao, new CelestrakClient(props.getProperty("celestrak.hostname")));
		tleReloader = new TLEReloader(props, tleDao, threadFactory, clock);
		signed = new SignedURL(props, clock);
		APTDecoder aptDecoder = new APTDecoder(props, processFactory);
		decoders.put("25338", aptDecoder);
		decoders.put("28654", aptDecoder);
		decoders.put("33591", aptDecoder);
		decoders.put("39430", new Gomx1Decoder(predict, props));
		decoders.put("39444", new Ao73Decoder(predict, props));
		decoders.put("40069", new LRPTDecoder(predict, props));
		decoders.put("41460", new Aausat4Decoder(predict, props));
		decoders.put("42017", new Nayif1Decoder(predict, props));
		decoders.put("42784", new PegasusDecoder(predict, props));
		decoders.put("42829", new TechnosatDecoder(predict, props));
		SnetDecoder snetDecoder = new SnetDecoder(predict, props);
		decoders.put("43186", snetDecoder);
		decoders.put("43187", snetDecoder);
		decoders.put("43188", snetDecoder);
		decoders.put("43189", snetDecoder);
		decoders.put("43466", new KunsPfDecoder(predict, props));
		decoders.put("43743", new ReaktorHelloWorldDecoder(predict, props));
		decoders.put("43786", new Itasat1Decoder(predict, props));
		decoders.put("43792", new EseoDecoder(predict, props));
		decoders.put("43798", new AstrocastDecoder(predict, props));
		decoders.put("43803", new Jy1satDecoder(predict, props));
		decoders.put("43804", new Suomi100Decoder(predict, props));
		decoders.put("43814", new PwSat2Decoder(predict, props));
		decoders.put("43881", new Dstar1Decoder(predict, props));
		decoders.put("43908", new Lume1Decoder(predict, props));
		decoders.put("44045", new Mysat1Decoder(predict, props));
		decoders.put("44103", new Aistechsat3Decoder(predict, props));
		decoders.put("44406", new Lucky7Decoder(predict, props));
		decoders.put("44830", new Atl1Decoder(predict, props));
		decoders.put("44832", new SmogPDecoder(predict, props));
		decoders.put("44878", new OpsSatDecoder(predict, props));
		decoders.put("44885", new Floripasat1Decoder(predict, props));

		validateDecoders();
		DecoderTask decoderTask = new DecoderTask(decoders, resultDao, r2cloudService);

		observationFactory = new ObservationFactory(predict, tleDao);
		scheduler = new Scheduler(new Schedule<>(), props, satelliteDao, rtlsdrLock, observationFactory, threadFactory, clock, processFactory, resultDao, decoderTask);

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
		index(new SSL(props, acmeClient));
		index(new SSLLog(acmeClient));
		index(new TLE(props, tleDao));
		index(new R2CloudSave(props));
		index(new ObservationSpectrogram(resultDao, spectogramService, signed));
		index(new ObservationList(satelliteDao, resultDao));
		index(new ObservationLoad(resultDao, signed, decoders));
		index(new ScheduleList(satelliteDao, scheduler));
		index(new ScheduleSave(satelliteDao, scheduler));
		index(new ScheduleStart(satelliteDao, scheduler));
		index(new ScheduleComplete(scheduler));
		webServer = new WebServer(props, controllers, auth, signed);
	}

	public void start() {
		acmeClient.start();
		ddnsClient.start();
		rtlsdrStatusDao.start();
		tleDao.start();
		tleReloader.start();
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
		tleReloader.stop();
		tleDao.stop();
		rtlsdrStatusDao.stop();
		ddnsClient.stop();
		acmeClient.stop();
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			LOG.info("invalid arguments. expected: config.properties");
			return;
		}
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
}
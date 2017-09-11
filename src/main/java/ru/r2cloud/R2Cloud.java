package ru.r2cloud;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.rx.ADSB;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ShutdownLoggingManager;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.HttpContoller;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.controller.ADSBData;
import ru.r2cloud.web.controller.DoLogin;
import ru.r2cloud.web.controller.DoRestore;
import ru.r2cloud.web.controller.DoSetup;
import ru.r2cloud.web.controller.GetAcmeLog;
import ru.r2cloud.web.controller.Login;
import ru.r2cloud.web.controller.Restore;
import ru.r2cloud.web.controller.SaveConfiguration;
import ru.r2cloud.web.controller.Setup;
import ru.r2cloud.web.controller.Status;
import ru.r2cloud.web.controller.StatusData;

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

	public R2Cloud(String propertiesLocation) {
		props = new Configuration(propertiesLocation);
		dao = new ADSBDao(props);
		adsb = new ADSB(props, dao);
		auth = new Authenticator(props);
		metrics = new Metrics(props);
		rtlsdrStatusDao = new RtlSdrStatusDao(props);
		autoUpdate = new AutoUpdate(props);
		ddnsClient = new DDNSClient(props);
		acmeClient = new AcmeClient(props);

		// setup web server
		index(new ru.r2cloud.web.controller.ADSB(props));
		index(new ADSBData(dao));
		index(new Login());
		index(new DoLogin(auth));
		index(new Setup());
		index(new DoSetup(auth, props));
		index(new Restore());
		index(new DoRestore(auth));
		index(new Status());
		index(new StatusData());
		index(new ru.r2cloud.web.controller.Configuration(props, autoUpdate, acmeClient));
		index(new SaveConfiguration(props, autoUpdate, ddnsClient, acmeClient));
		index(new GetAcmeLog(acmeClient));
		webServer = new WebServer(props, controllers, auth);
	}

	public void start() {
		metrics.start();
		if ("true".equals(props.getProperty("rx.adsb.enabled"))) {
			dao.start();
			adsb.start();
		} else {
			LOG.info("adsb is disabled");
		}
		acmeClient.start();
		ddnsClient.start();
		rtlsdrStatusDao.start();
		webServer.start();
		LOG.info("=================================");
		LOG.info("=========== started =============");
		LOG.info("=================================");
	}

	public void stop() {
		dao.stop();
		adsb.stop();
		acmeClient.stop();
		ddnsClient.stop();
		rtlsdrStatusDao.stop();
		webServer.stop();
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
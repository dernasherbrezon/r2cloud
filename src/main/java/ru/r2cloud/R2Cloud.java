package ru.r2cloud;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.r2cloud.rx.ADSB;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.uitl.ShutdownLoggingManager;
import ru.r2cloud.web.HttpContoller;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.controller.ADSBData;
import ru.r2cloud.web.controller.Home;

public class R2Cloud {

	static {
		// must be called before any Logger method is used.
		System.setProperty("java.util.logging.manager", ShutdownLoggingManager.class.getName());
	}

	private static final Logger LOG = Logger.getLogger(R2Cloud.class.getName());

	private Properties props = new Properties();

	private final Map<String, HttpContoller> controllers = new HashMap<String, HttpContoller>();
	private WebServer webServer;
	private ADSB adsb;
	private ADSBDao dao;

	public R2Cloud() {
		String propName = "/config.properties";
		try (InputStream is = R2Cloud.class.getResourceAsStream(propName)) {
			if (is == null) {
				throw new RuntimeException("unable to find properties: " + propName);
			}
			props.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
		dao = new ADSBDao(props);
		adsb = new ADSB(props, dao);

		// setup web server
		index(new Home());
		index(new ADSBData(dao));
		webServer = new WebServer(props, controllers);
	}

	public void start() {
		if ("true".equals(props.getProperty("rx.adsb.enabled"))) {
			dao.start();
			adsb.start();
		}
		webServer.start();
		LOG.info("=================================");
		LOG.info("=========== started =============");
		LOG.info("=================================");
	}

	public void stop() {
		dao.stop();
		adsb.stop();
		webServer.stop();
	}

	public static void main(String[] args) {
		R2Cloud app = new R2Cloud();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					LOG.info("stopping");
					app.stop();
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "unable to gracefully shutdown", e);
				} finally {
					LOG.info("=========== stopped =============");
					ShutdownLoggingManager.resetFinally();
				}
			}
		});
		app.start();
	}

	private void index(HttpContoller controller) {
		controllers.put(controller.getRequestMappingURL(), controller);
	}
}
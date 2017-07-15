package ru.r2cloud;

import java.io.InputStream;
import java.util.Properties;

public class R2Cloud {

	private Properties props = new Properties();

	private WebServer webServer;

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
		webServer = new WebServer(props.getProperty("server.hostname"), Integer.valueOf(props.getProperty("server.port")));
	}

	public void start() {
		webServer.start();
	}

	public void stop() {
		webServer.stop();
	}

	public static void main(String[] args) {
		R2Cloud app = new R2Cloud();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				app.stop();
			}
		});
		app.start();
	}

}
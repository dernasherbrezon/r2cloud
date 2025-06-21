package ru.r2cloud.cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class GpsdClient {

	private final static Logger LOG = LoggerFactory.getLogger(GpsdClient.class);

	private final Configuration config;
	private final Object lock = new Object();
	private boolean haveResult = false;
	private Double lat;
	private Double lon;
	private Double alt;

	public GpsdClient(Configuration config) {
		this.config = config;
	}

	public void updateCoordinates() {
		if (!config.getBoolean("location.auto")) {
			return;
		}
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				updateCoordinatesAsync();
			}
		}, "gpsd-client");
		t.start();
		synchronized (lock) {
			// ignore spurious wake-up
			if (!haveResult) {
				try {
					lock.wait(config.getInteger("location.gpsd.fixTimeout"));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			if (lat == null || lon == null) {
				LOG.info("can't acquire GPS fix in time. using old position. latitude: {} longitude: {} altitude: {}", config.getDouble("locaiton.lat"), config.getDouble("locaiton.lon"), config.getDouble("locaiton.alt"));
				if (!haveResult) {
					t.interrupt();
				}
				haveResult = false;
				return;
			}
			haveResult = false;
			LOG.info("acquired new position. latitude: {} longitude: {} altitude: {}", lat, lon, alt);
			config.setProperty("locaiton.lat", lat);
			config.setProperty("locaiton.lon", lon);
			config.setProperty("locaiton.alt", alt);
			config.update();
			// nullify to potentially re-use GpsdClient
			lat = null;
			lon = null;
			alt = null;
		}
	}

	private void updateCoordinatesAsync() {
		String hostname = config.getProperty("location.gpsd.hostname");
		int port = config.getInteger("location.gpsd.port");
		LOG.info("Connecting to GPSD to get coordinates: {}:{}", hostname, port);
		Socket socket = null;
		String version = null;
		try {
			socket = new Socket(hostname, port);
			socket.setSoTimeout(config.getInteger("location.gpsd.timeout"));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
			JsonObject req = new JsonObject();
			req.set("enable", true);
			req.set("json", true);
			req.set("class", "WATCH");
			writer.append("?WATCH=").append(req.toString()).append('\n');
			writer.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
			String curLine = null;
			while (!Thread.currentThread().isInterrupted() && (curLine = reader.readLine()) != null) {
				JsonObject obj = Json.parse(curLine).asObject();
				String clazz = obj.getString("class", null);
				if (clazz == null) {
					continue;
				}
				if (clazz.equals("VERSION")) {
					version = obj.getString("version", null);
					if (version == null) {
						version = obj.getString("release", null);
					}
					LOG.info("connected to gpsd. version: {}", version);
					continue;
				}
				if (clazz.equals("ERROR")) {
					LOG.error("response from gpsd: {}", obj.getString("message", null));
					continue;
				}
				if (!clazz.equals("TPV")) {
					continue;
				}
				int mode = obj.getInt("mode", 0);
				if (mode < 2) {
					// Unknown and no fix
					continue;
				}
				JsonValue lat = obj.get("lat");
				JsonValue lon = obj.get("lon");
				if (lat == null || lon == null) {
					// have fix but not coordinates?
					continue;
				}
				// PredictOreKit use WGS84 model
				JsonValue alt = obj.get("altHAE");
				synchronized (lock) {
					haveResult = true;
					this.lat = lat.asDouble();
					this.lon = lon.asDouble();
					if (alt != null) {
						this.alt = alt.asDouble();
					}
					lock.notifyAll();
				}
				break;
			}
		} catch (IOException e) {
			// non-null version means we have connection, but gpsd don't have any data
			// coming from serial connection. Maybe misconfiguration or failed hardware
			// will be logged later on
			if (version == null) {
				Util.logIOException(LOG, false, "unable to connect to GPSD", e);
			}
			synchronized (lock) {
				haveResult = true;
				lat = null;
				lon = null;
				alt = null;
				lock.notifyAll();
			}
		} finally {
			Util.closeQuietly(socket);
		}
	}

}

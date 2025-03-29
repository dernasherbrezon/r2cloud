package ru.r2cloud.cloud;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.TimeScalesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class InfluxDBClient {

	private final static Pattern ESCAPE = Pattern.compile("[\\s=\\,]+");
	private final static Logger LOG = LoggerFactory.getLogger(InfluxDBClient.class);

	private final Clock clock;
	private final HttpClient httpclient;
	private String influxDbHostname;
	private final Integer port;
	private final Duration timeout;
	private final String basicAuth;
	private final String database;
	private String hostname;

	public InfluxDBClient(Configuration config, Clock clock) {
		this.clock = clock;
		String influxDbHostname = config.getProperty("influxdb.hostname");
		port = config.getInteger("influxdb.port");
		timeout = Duration.ofMillis(config.getInteger("influxdb.timeout"));
		String username = config.getProperty("influxdb.username");
		String password = config.getProperty("influxdb.password");
		database = config.getProperty("influxdb.database");
		if (influxDbHostname == null || port == null || username == null || password == null || database == null) {
			LOG.info("some influxdb parameters not configured. skip sending metrics");
			this.httpclient = null;
			this.basicAuth = null;
			return;
		}
		if (!influxDbHostname.startsWith("http://")) {
			this.influxDbHostname = "http://" + influxDbHostname;
		} else {
			this.influxDbHostname = influxDbHostname;
		}
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(timeout).build();
		this.basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		this.hostname = config.getProperty("local.hostname");
		if (hostname == null) {
			try {
				this.hostname = escape(InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void send(Observation obs, Satellite satellite) {
		if (this.httpclient == null) {
			return;
		}
		StringBuilder metric = new StringBuilder();
		metric.append("observation");
		metric.append(",satellite=").append(escape(satellite.getName()));
		metric.append(",deviceId=").append(escape(obs.getDevice().getId()));
		metric.append(",antennaType=").append(obs.getDevice().getAntennaConfiguration().getType().toString());
		metric.append(",hostname=").append(hostname);
		metric.append(" tleUpdateLatency=").append(obs.getStartTimeMillis() - obs.getTle().getLastUpdateTime());

		TLE tle = new TLE(obs.getTle().getRaw()[1], obs.getTle().getRaw()[2]);
		long tleEpoch = tle.getDate().toDate(TimeScalesFactory.getUTC()).getTime();
		metric.append(",tleEpochLatency=").append(obs.getStartTimeMillis() - tleEpoch);

		Long frames = obs.getNumberOfDecodedPackets();
		if (frames == null) {
			frames = 0L;
		}
		metric.append(",numberOfDecodedPackets=").append(frames);
		Long totalSize = obs.getTotalSize();
		if (totalSize == null) {
			totalSize = 0L;
		}
		metric.append(",totalSize=").append(totalSize);
		metric.append(",duration=").append(obs.getEndTimeMillis() - obs.getStartTimeMillis());
		metric.append(" ").append(obs.getStartTimeMillis()).append("000000"); // nanoseconds

		HttpRequest request = createRequest("/write?db=" + this.database).header("Content-Type", "text/plain").POST(BodyPublishers.ofString(metric.toString())).build();
		httpclient.sendAsync(request, BodyHandlers.ofString()).exceptionally(e -> {
			Util.logIOException(LOG, false, "unable to send metric", e);
			return null;
		}).whenComplete((result, exception) -> {
			if (result.statusCode() != 200 && result.statusCode() != 204) {
				LOG.warn("unable to send metrics: {} - {}", result.statusCode(), result.body());
			}
		});
	}

	public void sendJvm() {
		if (this.httpclient == null) {
			return;
		}
		StringBuilder metric = new StringBuilder();
		metric.append("jvm");
		metric.append(",hostname=").append(hostname);
		metric.append(" heapMemory=").append(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
		metric.append(",totalThreads=").append(ManagementFactory.getThreadMXBean().getThreadCount());
		metric.append(" ").append(clock.millis()).append("000000"); // nanoseconds

		HttpRequest request = createRequest("/write?db=" + this.database).header("Content-Type", "text/plain").POST(BodyPublishers.ofString(metric.toString())).build();
		httpclient.sendAsync(request, BodyHandlers.ofString()).exceptionally(e -> {
			Util.logIOException(LOG, false, "unable to send metric", e);
			return null;
		}).whenComplete((result, exception) -> {
			if (result.statusCode() != 200 && result.statusCode() != 204) {
				LOG.warn("unable to send metrics: {} - {}", result.statusCode(), result.body());
			}
		});
	}

	private static String escape(String str) {
		return ESCAPE.matcher(str).replaceAll("");
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(influxDbHostname + ":" + port + path));
		result.timeout(timeout);
		result.header("User-Agent", R2Cloud.getVersion() + " r2cloud");
		result.header("Authorization", basicAuth);
		return result;
	}

}

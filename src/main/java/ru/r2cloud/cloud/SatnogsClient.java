package ru.r2cloud.cloud;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.jradio.ax25.Ax25Beacon;
import ru.r2cloud.jradio.csp.CspBeacon;
import ru.r2cloud.jradio.mobitex.MobitexBeacon;
import ru.r2cloud.jradio.tubix20.TUBiX20Beacon;
import ru.r2cloud.jradio.usp.UspBeacon;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteSource;
import ru.r2cloud.model.SatnogsTransmitterKey;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterKey;
import ru.r2cloud.model.TransmitterStatus;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatnogsClient {

	private static final Logger LOG = LoggerFactory.getLogger(SatnogsClient.class);
	private static final int MAX_RETRIES = 3;
	private static final long GUARANTEED_PERIOD = 1000L;

	private final HttpClient httpclient;
	private final String hostname;
	private final Duration timeout;
	private final Clock clock;

	public SatnogsClient(Configuration config, Clock clock) {
		this.hostname = config.getProperty("satnogs.hostname");
		this.clock = clock;
		this.timeout = Duration.ofMillis(config.getInteger("satnogs.connectionTimeout"));
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(timeout).build();
	}

	public List<Satellite> loadSatellites() {
		LOG.info("loading satellites from satnogs");
		List<Transmitter> transmitters = loadAllTransmitters();
		if (transmitters.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<Transmitter>> groupBySatelliteId = new HashMap<>();
		for (Transmitter cur : transmitters) {
			List<Transmitter> old = groupBySatelliteId.get(cur.getSatelliteId());
			if (old == null) {
				old = new ArrayList<>();
				groupBySatelliteId.put(cur.getSatelliteId(), old);
			}
			old.add(cur);
		}
		List<Satellite> result = loadAllSatellites(groupBySatelliteId);
		for (Satellite cur : result) {
			dedupTransmittersByKey(cur);
		}
		LOG.info("satellites from satnogs were loaded: {}", result.size());
		return result;
	}

	private Tle loadTleBySatelliteId(String satelliteId) {
		Builder request = HttpRequest.newBuilder().uri(URI.create(hostname + "/api/tle/?sat_id=" + satelliteId));
		request.timeout(timeout);
		request.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		try {
			HttpResponse<String> response = sendWithRetry(request.GET().build(), BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to load tle. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return null;
			}
			return readTle(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load tle", e);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private List<Satellite> loadAllSatellites(Map<String, List<Transmitter>> groupBySatelliteId) {
		Builder request = HttpRequest.newBuilder().uri(URI.create(hostname + "/api/satellites/"));
		request.timeout(timeout);
		request.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		try {
			HttpResponse<String> response = sendWithRetry(request.GET().build(), BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to load satellites. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return Collections.emptyList();
			}
			return readSatellites(response.body(), groupBySatelliteId);
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load satellites from satnogs", e);
			return Collections.emptyList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private Tle readTle(String body) {
		JsonValue parsedJson;
		try {
			parsedJson = Json.parse(body);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return null;
		}
		if (!parsedJson.isArray()) {
			LOG.info("malformed json");
			return null;
		}
		JsonArray parsedArray = parsedJson.asArray();
		if (parsedArray.isEmpty()) {
			return null;
		}
		JsonObject tle = parsedArray.get(0).asObject();

		Tle result = new Tle(new String[] { getStringSafely(tle, "tle0"), getStringSafely(tle, "tle1"), getStringSafely(tle, "tle2") });
		// ignore "updated" field from satnogs
		// if satellite is new launch, then there is no other source for tle
		// readTle is called only for new launches
		result.setLastUpdateTime(clock.millis());
		result.setSource(hostname);
		return result;
	}

	private List<Satellite> readSatellites(String body, Map<String, List<Transmitter>> groupBySatelliteId) {
		JsonValue parsedJson;
		try {
			parsedJson = Json.parse(body);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		if (!parsedJson.isArray()) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		JsonArray parsedArray = parsedJson.asArray();
		List<Satellite> result = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat updatedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		updatedFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		for (int i = 0; i < parsedArray.size(); i++) {
			JsonValue jsonValue = parsedArray.get(i);
			if (!jsonValue.isObject()) {
				continue;
			}
			JsonObject satellite = jsonValue.asObject();
			long noradId = getLongSafely(satellite, "norad_cat_id");
			String status = getStringSafely(satellite, "status");
			if (status == null) {
				continue;
			}
			String id = getStringSafely(satellite, "sat_id");
			if (id == null) {
				continue;
			}
			String name = getStringSafely(satellite, "name");
			if (name == null) {
				continue;
			}
			List<Transmitter> transmitters = groupBySatelliteId.get(id);
			if (transmitters == null || transmitters.isEmpty()) {
				continue;
			}

			Satellite cur = new Satellite();
			cur.setName(name);
			cur.setTransmitters(transmitters);
			cur.setSource(SatelliteSource.SATNOGS);
			// can't detect if satellite is just launched or junk data
			// thus disabled by default even if "new launch"
			// https://gitlab.com/librespacefoundation/satnogs/satnogs-db/-/issues/551
			cur.setEnabled(false);
			cur.setTle(loadTleBySatelliteId(id));
			String updatedStr = getStringSafely(satellite, "updated");
			if (updatedStr != null) {
				try {
					updatedStr = updatedStr.substring(0, updatedStr.length() - 4);
					cur.setLastUpdateTime(updatedFormat.parse(updatedStr).getTime());
				} catch (java.text.ParseException e) {
					// ignore
				}
			}
			// schedule observations anyway
			if (status.equalsIgnoreCase("future")) {
				cur.setId(id);
				cur.setPriority(Priority.HIGH);
				String launchedStr = getStringSafely(satellite, "launched");
				if (launchedStr != null) {
					continue;
				}
				try {
					cur.setStart(sdf.parse(launchedStr));
				} catch (Exception e) {
					continue;
				}
			} else if (status.equalsIgnoreCase("alive")) {
				if (noradId == 0) {
					continue;
				}
				if (noradId > 90000) {
					cur.setId(id);
					cur.setPriority(Priority.HIGH);
					// schedule immediately
					cur.setStart(new Date(clock.millis() - 10000));
				} else {
					cur.setId(String.valueOf(noradId));
					cur.setPriority(Priority.NORMAL);
				}
			} else {
				// ignore dead or re-entered
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	private List<Transmitter> loadAllTransmitters() {
		Builder request = HttpRequest.newBuilder().uri(URI.create(hostname + "/api/transmitters/?type=Transmitter"));
		request.timeout(timeout);
		request.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		try {
			HttpResponse<String> response = sendWithRetry(request.GET().build(), BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to load transmitters. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return Collections.emptyList();
			}
			return groupByBaudRate(readTransmitters(response.body()));
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load transmitters", e);
			return Collections.emptyList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static List<Transmitter> groupByBaudRate(List<Transmitter> all) {
		Map<TransmitterKey, Transmitter> grouped = new HashMap<>();
		for (Transmitter cur : all) {
			TransmitterKey key = new TransmitterKey(cur);
			Transmitter previous = grouped.get(key);
			if (previous == null) {
				grouped.put(key, cur);
				continue;
			}
			previous.getBaudRates().addAll(cur.getBaudRates());
			previous.setBandwidth(Math.max(previous.getBandwidth(), cur.getBandwidth()));
		}
		return new ArrayList<>(grouped.values());
	}

	private static List<Transmitter> readTransmitters(String body) {
		JsonValue parsedJson;
		try {
			parsedJson = Json.parse(body);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		if (!parsedJson.isArray()) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		JsonArray parsedArray = parsedJson.asArray();
		List<Transmitter> result = new ArrayList<>();
		for (int i = 0; i < parsedArray.size(); i++) {
			JsonValue jsonValue = parsedArray.get(i);
			if (!jsonValue.isObject()) {
				continue;
			}

			JsonObject transmitter = jsonValue.asObject();
			boolean alive = transmitter.getBoolean("alive", false);
			if (!alive) {
				continue;
			}
			String description = getStringSafely(transmitter, "description");
			if (description == null) {
				continue;
			}
			description = description.toLowerCase();
			String modulationStr = getStringSafely(transmitter, "mode");
			if (modulationStr == null) {
				continue;
			}
			long frequency = getLongSafely(transmitter, "downlink_low");
			if (frequency == 0) {
				continue;
			}
			float baudRate = getFloatSafely(transmitter, "baud");
			if (baudRate == 0.0) {
				continue;
			}
			Modulation modulation = convert(modulationStr);
			if (modulation == null) {
				continue;
			}
			String satelliteId = getStringSafely(transmitter, "sat_id");
			if (satelliteId == null) {
				continue;
			}
			Transmitter cur = new Transmitter();
			List<Integer> baudRates = new ArrayList<>();
			baudRates.add((int) baudRate);
			cur.setBaudRates(baudRates);
			switch ((int) baudRate) {
			case 9600:
				cur.setBandwidth(20000);
				break;
			case 4800:
				cur.setBandwidth(15000);
				break;
			case 2400:
				cur.setBandwidth(10000);
				break;
			case 1200:
				if (modulation.equals(Modulation.AFSK)) {
					cur.setBandwidth(20000);
				} else {
					cur.setBandwidth(10000);
				}
				break;
			default:
				cur.setBandwidth((int) (baudRate * 2));
				break;
			}
			cur.setFrequency(frequency);
			cur.setModulation(modulation);
			cur.setStatus(TransmitterStatus.ENABLED);
			cur.setSatelliteId(satelliteId);
			cur.setTransitionWidth(2000);
			cur.setDeviation(5000);
			if (description.contains(" usp ")) {
				cur.setFraming(Framing.USP);
				cur.setBeaconClass(UspBeacon.class);
			} else if (description.contains("ax.25 g3ruh") || description.contains("g3ruh ax.25") || description.contains("ax25 g3ruh") || description.contains("g3ruh ax25")) {
				cur.setFraming(Framing.AX25G3RUH);
				cur.setBeaconClass(Ax25Beacon.class);
			} else if (description.contains("ax.25") || description.contains("ax25")) {
				cur.setFraming(Framing.AX25);
				cur.setBeaconClass(Ax25Beacon.class);
			} else if (description.contains("mobitex-nx")) {
				cur.setFraming(Framing.TUBIX20);
				cur.setBeaconClass(TUBiX20Beacon.class);
			} else if (description.contains("mobitex")) {
				cur.setFraming(Framing.MOBITEX);
				cur.setBeaconClass(MobitexBeacon.class);
			}
			if (modulationStr.contains(" AX.100 ")) {
				cur.setFraming(Framing.AX100);
				cur.setBeaconClass(CspBeacon.class);
				cur.setBeaconSizeBytes(255);
			}

			if (cur.getFraming() == null || cur.getBeaconClass() == null) {
				continue;
			}

			String updatedStr = getStringSafely(transmitter, "updated");
			if (updatedStr != null && updatedStr.length() >= 23) {
				try {
					cur.setUpdated(sdf.parse(updatedStr.substring(0, 23)));
				} catch (java.text.ParseException e) {
					// ignore
				}
			}

			result.add(cur);
		}
		return result;
	}

	private static String getStringSafely(JsonObject obj, String fieldName) {
		try {
			return obj.getString(fieldName, null);
		} catch (Exception e) {
			return null;
		}
	}

	private static long getLongSafely(JsonObject obj, String fieldName) {
		JsonValue value = obj.get(fieldName);
		if (value == null || !value.isNumber()) {
			return 0;
		}
		try {
			return value.asLong();
		} catch (Exception e) {
			return 0;
		}
	}

	private static float getFloatSafely(JsonObject obj, String fieldName) {
		JsonValue value = obj.get(fieldName);
		if (value == null || !value.isNumber()) {
			return 0;
		}
		try {
			return value.asFloat();
		} catch (Exception e) {
			return 0;
		}
	}

	private static void dedupTransmittersByKey(Satellite satellite) {
		if (satellite.getTransmitters() == null || satellite.getTransmitters().isEmpty()) {
			return;
		}
		Map<SatnogsTransmitterKey, Transmitter> byKey = new HashMap<>();
		for (Transmitter cur : satellite.getTransmitters()) {
			SatnogsTransmitterKey key = new SatnogsTransmitterKey(cur);
			Transmitter old = byKey.get(key);
			if (old == null) {
				byKey.put(key, cur);
			} else {
				// replace transmitter with newer update timestamp
				if (cur.getUpdated() != null && old.getUpdated() != null && cur.getUpdated().after(old.getUpdated())) {
					byKey.put(key, cur);
				}
			}
		}
		satellite.setTransmitters(new ArrayList<>(byKey.values()));
	}

	private static Modulation convert(String modulation) {
		if (modulation.equalsIgnoreCase("GMSK") || modulation.equalsIgnoreCase("GFSK") || modulation.equalsIgnoreCase("FSK") || modulation.startsWith("FSK ") || modulation.startsWith("MSK ")) {
			return Modulation.GFSK;
		}
		if (modulation.equalsIgnoreCase("BPSK")) {
			return Modulation.BPSK;
		}
		if (modulation.equalsIgnoreCase("AFSK")) {
			return Modulation.AFSK;
		}
		// satnogs doesn't have enough parameters for lora transmitters
//		if (modulation.equalsIgnoreCase("LORA")) {
//			return Modulation.LORA;
//		}
		if (modulation.equalsIgnoreCase("QPSK")) {
			return Modulation.QPSK;
		}
		return null;
	}

	private HttpResponse<String> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) throws IOException, InterruptedException {
		int currentRetry = 0;
		while (true) {
			try {
				HttpResponse<String> result = httpclient.send(request, responseBodyHandler);
				// retry for any server-side errors
				// can happen during server upgrade
				if (result.statusCode() < 500) {
					return result;
				}
				currentRetry++;
				if (currentRetry > MAX_RETRIES) {
					return result;
				}
				LOG.info("unable to send. status code: {}. retry {}", result.statusCode(), currentRetry);
			} catch (IOException e) {
				currentRetry++;
				if (currentRetry > MAX_RETRIES) {
					throw e;
				}
				Util.logIOException(LOG, false, "unable to send. retry " + currentRetry, e);
			}
			// linear backoff with random jitter
			Thread.sleep(GUARANTEED_PERIOD * currentRetry + (long) (Math.random() * GUARANTEED_PERIOD * currentRetry));
		}
	}

}

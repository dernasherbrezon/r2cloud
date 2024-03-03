package ru.r2cloud.web.api.observation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.MimeType;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ObservationSigMfMeta extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationSigMfMeta.class);

	private final IObservationDao resultDao;
	private final Configuration config;
	private final SignedURL signed;
	private final SatelliteDao satelliteDao;
	private final PredictOreKit predict;

	public ObservationSigMfMeta(Configuration config, IObservationDao resultDao, SignedURL signed, SatelliteDao satelliteDao, PredictOreKit predict) {
		this.config = config;
		this.resultDao = resultDao;
		this.signed = signed;
		this.satelliteDao = satelliteDao;
		this.predict = predict;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ValidationResult errors = new ValidationResult();
		String id = WebServer.getParameter(session, "id");
		if (id == null) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}
		String satelliteId = WebServer.getParameter(session, "satelliteId");
		if (satelliteId == null) {
			errors.put("satelliteId", Messages.CANNOT_BE_EMPTY);
		}

		if (!errors.isEmpty()) {
			return new BadRequest(errors);
		}

		String uri = session.getUri() + "?satelliteId=" + satelliteId + "&id=" + id;
		if (!signed.validate(uri, WebServer.getParameters(session))) {
			return new ModelAndView(NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "invalid signed url"));
		}

		Observation entity = resultDao.find(satelliteId, id);
		if (entity == null || !entity.getRawPath().exists()) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}
		Satellite satellite = satelliteDao.findById(entity.getSatelliteId());
		if (satellite == null) {
			LOG.info("satellite not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}
		Transmitter transmitter = satellite.getById(entity.getTransmitterId());
		if (transmitter == null) {
			LOG.info("transmitted not found: {} id: {}", entity.getTransmitterId(), id);
			return new NotFound();
		}

		ModelAndView result = new ModelAndView();
		SimpleDateFormat dateFormat = createParser();
		Long ifModifiedSince = getIfModifiedSince(session);
		Response response;
		if (ifModifiedSince != null && ifModifiedSince >= entity.getRawPath().lastModified() / 1000) {
			response = NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.NOT_MODIFIED, "application/octet-stream", null);
		} else {
			response = NanoHTTPD.newFixedLengthResponse(Status.OK, MimeType.JSON.getType(), convertToSigMfMeta(entity, satellite, transmitter).toString());
			// convert to seconds
			response.addHeader("Cache-Control", "private, max-age=" + ((int) (config.getLong("server.static.signed.validMillis") / 1000)));
		}
		response.addHeader("Content-Disposition", "attachment; filename=r2cloud-" + id + ".sigmf-meta");
		response.addHeader("Last-Modified", dateFormat.format(new Date(entity.getRawPath().lastModified())));
		result.setRaw(response);
		return result;

	}

	private JsonObject convertToSigMfMeta(Observation observation, Satellite satellite, Transmitter transmitter) {
		JsonObject global = new JsonObject();
		global.add("core:author", "r2cloud");
		global.add("core:description", "Automatic recording from satellite " + satellite.getName() + " (" + satellite.getId() + ")");
		global.add("core:version", "1.0.0");
		global.add("core:recorder", "r2cloud");
		global.add("core:sample_rate", observation.getSampleRate());
		global.add("core:license", "https://creativecommons.org/licenses/by/4.0/");

		JsonArray coordinates = new JsonArray();
		// make private for sharing data safely
		coordinates.add(((long) ((observation.getGroundStation().getLongitude() * 180.0f / Math.PI) * 100)) / 100.0f);
		coordinates.add(((long) ((observation.getGroundStation().getLatitude() * 180.0f / Math.PI) * 100)) / 100.0f);
		coordinates.add(observation.getGroundStation().getAltitude());

		JsonObject geolocation = new JsonObject();
		geolocation.add("type", "Point");
		geolocation.add("coordinates", coordinates);

		global.add("core:geolocation", geolocation);

		switch (observation.getDataFormat()) {
		case COMPLEX_FLOAT:
			global.add("core:datatype", "cf32_le");
			break;
		case COMPLEX_SIGNED_SHORT:
			global.add("core:datatype", "ci16_le");
			break;
		case COMPLEX_UNSIGNED_BYTE:
			global.add("core:datatype", "cu8");
			break;
		default:
			// what would happen if datatype is unknown?
			break;
		}

		JsonObject r2cloudSatellite = new JsonObject();
		r2cloudSatellite.set("noradId", satellite.getId());
		r2cloudSatellite.set("name", satellite.getName());
		JsonObject r2cloudTle = observation.getTle().toJson();
		r2cloudSatellite.set("tle", r2cloudTle);
		global.set("r2cloud:satellite", r2cloudSatellite);
		JsonObject transmitterJson = transmitter.toJson();
		transmitterJson.remove("status");
		global.set("r2cloud:signal", transmitterJson);
		if (observation.getDevice() != null) {
			JsonObject deviceConfiguration = observation.getDevice().toJson();
			deviceConfiguration.remove("username");
			deviceConfiguration.remove("password");
			deviceConfiguration.remove("host");
			deviceConfiguration.remove("port");
			JsonValue rotator = deviceConfiguration.get("rotator");
			if (rotator != null) {
				JsonObject rotatorObj = rotator.asObject();
				rotatorObj.remove("rotctrldHostname");
				rotatorObj.remove("rotctrldPort");
			}
			global.set("r2cloud:device", deviceConfiguration);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		JsonObject capture = new JsonObject();
		capture.add("core:datetime", sdf.format(new Date(observation.getStartTimeMillis())));
		capture.add("core:frequency", observation.getFrequency());
		capture.add("core:sample_start", 0);

		JsonArray captures = new JsonArray();
		captures.add(capture);

		JsonObject result = new JsonObject();
		result.add("global", global);
		result.add("captures", captures);
		result.add("annotations", convertBeacons(observation));
		return result;
	}

	private JsonArray convertBeacons(Observation entity) {
		if (entity.getDataPath() == null) {
			return new JsonArray();
		}
		Satellite satellite = satelliteDao.findById(entity.getSatelliteId());
		if (satellite == null) {
			return new JsonArray();
		}
		Transmitter transmitter = satellite.getById(entity.getTransmitterId());
		if (transmitter == null) {
			return new JsonArray();
		}
		Class<? extends Beacon> clazz = transmitter.getBeaconClass();
		if (clazz == null) {
			return new JsonArray();
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(entity.getTle().getRaw()[1], entity.getTle().getRaw()[2]));
		TopocentricFrame groundStation = predict.getPosition(entity.getGroundStation());
		try (BeaconInputStream<?> ais = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(entity.getDataPath())), clazz)) {
			JsonArray data = new JsonArray();
			while (ais.hasNext()) {
				Beacon b = ais.next();
				// old storage format for beacons
				// discard beacons without end sample
				if (b.getEndSample() < b.getBeginSample()) {
					continue;
				}
				long actualFrequency = predict.getDownlinkFreq(transmitter.getFrequency(), b.getBeginMillis(), groundStation, tlePropagator);
				JsonObject annotation = new JsonObject();
				annotation.add("core:description", "Beacon");
				annotation.add("core:freq_lower_edge", actualFrequency - b.getRxMeta().getBaud() / 2);
				annotation.add("core:freq_upper_edge", actualFrequency + b.getRxMeta().getBaud() / 2);
				annotation.add("core:sample_count", b.getEndSample() - b.getBeginSample());
				annotation.add("core:sample_start", b.getBeginSample());
				data.add(annotation);
			}
			return data;
		} catch (Exception e) {
			LOG.error("unable to read binary data", e);
			return new JsonArray();
		}
	}

	private static Long getIfModifiedSince(IHTTPSession session) {
		String ifModifiedSince = session.getHeaders().get("if-modified-since");
		if (ifModifiedSince == null) {
			return null;
		}
		try {
			return createParser().parse(ifModifiedSince).getTime();
		} catch (Exception e) {
			return null;
		}
	}

	private static SimpleDateFormat createParser() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/static/observation/sigmf/meta";
	}

}

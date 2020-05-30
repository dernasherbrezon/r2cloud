package ru.r2cloud.web.api.observation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.model.Observation;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.decoder.TelemetryDecoder;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ObservationLoad extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationLoad.class);

	private final ObservationResultDao resultDao;
	private final SignedURL signed;
	private final Map<String, Decoder> decoders;

	public ObservationLoad(ObservationResultDao resultDao, SignedURL signed, Map<String, Decoder> decoders) {
		this.resultDao = resultDao;
		this.signed = signed;
		this.decoders = decoders;
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

		Observation entity = resultDao.find(satelliteId, id);
		if (entity == null) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}
		JsonObject json = entity.toJson(signed);
		if (entity.getDataPath() != null) {
			Decoder decoder = decoders.get(entity.getSatelliteId());
			if (decoder instanceof TelemetryDecoder) {
				TelemetryDecoder telemetryDecoder = (TelemetryDecoder) decoder;
				try (BeaconInputStream<?> ais = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(entity.getDataPath())), telemetryDecoder.getBeaconClass())) {
					JsonArray data = new JsonArray();
					while (ais.hasNext()) {
						data.add(convert(ais.next()));
					}
					json.add("dataEntity", data);
				} catch (Exception e) {
					LOG.error("unable to read binary data", e);
				}
			}
		}
		ModelAndView result = new ModelAndView();
		result.setData(json.toString());
		return result;
	}

	private static JsonObject convert(Beacon b) {
		JsonObject result = new JsonObject();
		result.add("name", b.getBeginMillis());
		JsonValue convertObject = Util.convertObject(b);
		if (convertObject != null) {
			convertObject.asObject().remove("rawData").remove("beginMillis").remove("beginSample");
			result.add("body", convertObject);
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/load";
	}

}

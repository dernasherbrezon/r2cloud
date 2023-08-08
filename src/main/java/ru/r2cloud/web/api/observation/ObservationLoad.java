package ru.r2cloud.web.api.observation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.RxMetadata;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
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

	private final IObservationDao resultDao;
	private final SignedURL signed;
	private final SatelliteDao satelliteDao;

	public ObservationLoad(IObservationDao resultDao, SignedURL signed, SatelliteDao satelliteDao) {
		this.resultDao = resultDao;
		this.signed = signed;
		this.satelliteDao = satelliteDao;
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
			JsonArray beacons = convertBeacons(entity);
			if (beacons != null) {
				json.add("dataEntity", beacons);
			}
		}
		ModelAndView result = new ModelAndView();
		result.setData(json.toString());
		return result;
	}

	private JsonArray convertBeacons(Observation entity) {
		Satellite satellite = satelliteDao.findById(entity.getSatelliteId());
		if (satellite == null) {
			return null;
		}
		Transmitter transmitter = satellite.getById(entity.getTransmitterId());
		if (transmitter == null) {
			return null;
		}
		Class<? extends Beacon> clazz = transmitter.getBeaconClass();
		if (clazz == null) {
			return null;
		}
		try (BeaconInputStream<?> ais = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(entity.getDataPath())), clazz)) {
			JsonArray data = new JsonArray();
			while (ais.hasNext()) {
				data.add(convert(ais.next()));
			}
			return data;
		} catch (Exception e) {
			LOG.error("unable to read binary data", e);
			return null;
		}
	}

	private static JsonObject convert(Beacon b) {
		JsonObject result = new JsonObject();
		result.add("time", b.getBeginMillis());
		JsonValue convertObject = Util.convertObject(b);
		if (convertObject != null) {
			convertObject.asObject().remove("rawData").remove("beginMillis").remove("beginSample").remove("rxMeta").remove("endSample");
			result.add("body", convertObject);
		}
		RxMetadata rxMeta = b.getRxMeta();
		if (rxMeta != null) {
			if (rxMeta.getFrequencyError() != null) {
				result.add("frequencyError", rxMeta.getFrequencyError());
			}
			if (rxMeta.getRssi() != null) {
				result.add("rssi", rxMeta.getRssi());
			}
			if (rxMeta.getSnr() != null) {
				result.add("snr", rxMeta.getSnr());
			}
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/load";
	}

}

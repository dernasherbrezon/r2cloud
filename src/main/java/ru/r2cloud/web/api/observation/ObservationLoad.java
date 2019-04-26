package ru.r2cloud.web.api.observation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.aausat4.AAUSAT4Beacon;
import ru.r2cloud.jradio.csp.Header;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.util.SignedURL;
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

	public ObservationLoad(ObservationResultDao resultDao, SignedURL signed) {
		this.resultDao = resultDao;
		this.signed = signed;
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

		ObservationFull entity = resultDao.find(satelliteId, id);
		if (entity == null) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}
		JsonObject json = entity.toJson(signed);
		if (entity.getResult().getDataPath() != null) {
			if (entity.getReq().getSatelliteId().equals("41460")) {
				try (BeaconInputStream<AAUSAT4Beacon> ais = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(entity.getResult().getDataPath())), AAUSAT4Beacon.class)) {
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

	private static JsonObject convert(AAUSAT4Beacon beacon) {
		JsonObject result = new JsonObject();
		result.add("name", beacon.getEps().getUptime());
		JsonObject body = new JsonObject();
		result.add("body", body);

		body.add("length", beacon.getLength());
		Header header = beacon.getHeader();
		body.add("priority", header.getPriority().name());
		body.add("ffrag", header.isFfrag());
		body.add("fhmac", header.isFhmac());
		body.add("fxtea", header.isFxtea());
		body.add("frdp", header.isFrdp());
		body.add("fcrc32", header.isFcrc32());

		JsonObject eps = new JsonObject();
		body.add("eps", eps);

		eps.add("bootCount", beacon.getEps().getBootCount());
		eps.add("uptime", beacon.getEps().getUptime());
		eps.add("realtimeClock", beacon.getEps().getRealtimeClock());
		eps.add("pingStatus", beacon.getEps().getPingStatus());
		eps.add("subsystemStatus", beacon.getEps().getSubsystemStatus());
		eps.add("batteryVoltage", beacon.getEps().getBatteryVoltage());
		eps.add("cellDiff", beacon.getEps().getCellDiff());
		eps.add("batteryCurrent", beacon.getEps().getBatteryCurrent());
		eps.add("solarPower", beacon.getEps().getSolarPower());
		eps.add("temperature", beacon.getEps().getTemperature());
		eps.add("paTemperature", beacon.getEps().getPaTemperature());
		eps.add("mainVoltage", beacon.getEps().getMainVoltage());

		JsonObject com = new JsonObject();
		body.add("com", com);

		com.add("bootCount", beacon.getCom().getBootCount());
		com.add("packetsReceived", beacon.getCom().getPacketsReceived());
		com.add("packetsSend", beacon.getCom().getPacketsSend());
		com.add("latestRssi", beacon.getCom().getLatestRssi());
		com.add("latestBitCorrection", beacon.getCom().getLatestBitCorrection());
		com.add("latestByteCorrection", beacon.getCom().getLatestByteCorrection());

		if (beacon.getAdcs1() != null) {
			JsonObject adcs1 = new JsonObject();
			body.add("adcs1", adcs1);

			adcs1.add("state", beacon.getAdcs1().getState());
			adcs1.add("bdot1", beacon.getAdcs1().getBdot1());
			adcs1.add("bdot2", beacon.getAdcs1().getBdot2());
			adcs1.add("bdot3", beacon.getAdcs1().getBdot3());
		}

		if (beacon.getAdcs2() != null) {
			JsonObject adcs2 = new JsonObject();
			body.add("adcs2", adcs2);

			adcs2.add("gyro1", beacon.getAdcs2().getGyro1());
			adcs2.add("gyro2", beacon.getAdcs2().getGyro2());
			adcs2.add("gyro3", beacon.getAdcs2().getGyro3());
		}

		if (beacon.getAis1() != null) {
			JsonObject ais1 = new JsonObject();
			body.add("ais1", ais1);

			ais1.add("bootCount", beacon.getAis1().getBootCount());
			ais1.add("uniqueMssi", beacon.getAis1().getUniqueMssi());
		}

		if (beacon.getAis1() != null) {
			JsonObject ais2 = new JsonObject();
			body.add("ais2", ais2);

			ais2.add("bootCount", beacon.getAis1().getBootCount());
			ais2.add("uniqueMssi", beacon.getAis1().getUniqueMssi());
		}

		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/load";
	}

}

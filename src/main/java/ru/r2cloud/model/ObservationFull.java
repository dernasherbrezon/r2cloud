package ru.r2cloud.model;

import java.util.Locale;

import org.orekit.bodies.GeodeticPoint;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.SignedURL;

public class ObservationFull {

	private final ObservationRequest req;

	private ObservationResult result;

	public ObservationFull(ObservationRequest req) {
		this.req = req;
	}

	public ObservationRequest getReq() {
		return req;
	}

	public ObservationResult getResult() {
		return result;
	}

	public void setResult(ObservationResult result) {
		this.result = result;
	}

	public static ObservationFull fromJson(JsonObject meta) {
		ObservationRequest req = new ObservationRequest();
		req.setId(meta.getString("id", null));
		req.setStartTimeMillis(meta.getLong("start", -1L));
		req.setEndTimeMillis(meta.getLong("end", -1L));
		req.setOutputSampleRate(meta.getInt("sampleRate", -1));
		req.setInputSampleRate(meta.getInt("inputSampleRate", -1));
		req.setSatelliteFrequency(meta.getLong("frequency", -1));
		req.setActualFrequency(meta.getLong("actualFrequency", -1));
		req.setBandwidth(meta.getLong("bandwidth", -1));
		String decoder = meta.getString("decoder", null);
		if ("aausat4".equals(decoder)) {
			decoder = "telemetry";
		}
		if (decoder != null) {
			req.setSource(FrequencySource.valueOf(decoder.toUpperCase(Locale.UK)));
		}
		req.setSatelliteId(meta.getString("satellite", null));
		JsonValue tle = meta.get("tle");
		if (tle != null && tle.isObject()) {
			req.setTle(Tle.fromJson(tle.asObject()));
		}
		JsonValue groundStation = meta.get("groundStation");
		if (groundStation != null && groundStation.isObject()) {
			req.setGroundStation(groundStationFromJson(groundStation.asObject()));
		}

		ObservationResult result = new ObservationResult();
		result.setGain(meta.getString("gain", null));
		result.setChannelA(meta.getString("channelA", null));
		result.setChannelB(meta.getString("channelB", null));
		result.setNumberOfDecodedPackets(meta.getLong("numberOfDecodedPackets", 0));
		result.setaURL(meta.getString("aURL", null));
		result.setDataURL(meta.getString("data", null));
		result.setSpectogramURL(meta.getString("spectogramURL", null));

		ObservationFull full = new ObservationFull(req);
		full.setResult(result);
		return full;
	}

	public JsonObject toJson(SignedURL signed) {
		JsonObject json = new JsonObject();
		json.add("id", req.getId());
		json.add("start", req.getStartTimeMillis());
		json.add("end", req.getEndTimeMillis());
		json.add("sampleRate", req.getOutputSampleRate());
		json.add("inputSampleRate", req.getInputSampleRate());
		json.add("frequency", req.getSatelliteFrequency());
		json.add("actualFrequency", req.getActualFrequency());
		json.add("decoder", req.getSource().name());
		json.add("satellite", req.getSatelliteId());
		json.add("bandwidth", req.getBandwidth());
		if (req.getTle() != null) {
			json.add("tle", req.getTle().toJson());
		}
		if (req.getGroundStation() != null) {
			json.add("groundStation", toJson(req.getGroundStation()));
		}

		if (result == null) {
			return json;
		}

		if (result.getGain() != null) {
			json.add("gain", result.getGain());
		}
		if (result.getChannelA() != null) {
			json.add("channelA", result.getChannelA());
		}
		if (result.getChannelB() != null) {
			json.add("channelB", result.getChannelB());
		}
		if (result.getNumberOfDecodedPackets() != null) {
			json.add("numberOfDecodedPackets", result.getNumberOfDecodedPackets());
		}
		if (result.getaURL() != null) {
			if (signed != null) {
				json.add("aURL", signed.sign(result.getaURL()));
			} else {
				json.add("aURL", result.getaURL());
			}
		}
		if (result.getDataURL() != null) {
			if (signed != null) {
				json.add("data", signed.sign(result.getDataURL()));
			} else {
				json.add("data", result.getDataURL());
			}
		}
		if (result.getSpectogramURL() != null) {
			if (signed != null) {
				json.add("spectogramURL", signed.sign(result.getSpectogramURL()));
			} else {
				json.add("spectogramURL", result.getSpectogramURL());
			}
		}
		return json;
	}

	private static GeodeticPoint groundStationFromJson(JsonObject json) {
		double lat = json.getDouble("lat", Double.NaN);
		double lon = json.getDouble("lon", Double.NaN);
		return new GeodeticPoint(lat, lon, 0.0);
	}

	private static JsonObject toJson(GeodeticPoint groundStation) {
		JsonObject result = new JsonObject();
		result.add("lat", groundStation.getLatitude());
		result.add("lon", groundStation.getLongitude());
		return result;
	}
}

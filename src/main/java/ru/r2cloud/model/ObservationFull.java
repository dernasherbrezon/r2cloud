package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

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
		req.setDecoder(meta.getString("decoder", null));
		req.setSatelliteId(meta.getString("satellite", null));

		ObservationResult result = new ObservationResult();
		result.setGain(meta.getString("gain", null));
		result.setChannelA(meta.getString("channelA", null));
		result.setChannelB(meta.getString("channelB", null));
		JsonValue decodedPackets = meta.get("numberOfDecodedPackets");
		if (decodedPackets != null) {
			result.setNumberOfDecodedPackets(decodedPackets.asLong());
		}
		result.setaURL(meta.getString("aURL", null));
		result.setDataURL(meta.getString("data", null));
		result.setSpectogramURL(meta.getString("spectogramURL", null));

		ObservationFull full = new ObservationFull(req);
		full.setResult(result);
		return full;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.add("id", req.getId());
		json.add("start", req.getStartTimeMillis());
		json.add("end", req.getEndTimeMillis());
		json.add("sampleRate", req.getOutputSampleRate());
		json.add("inputSampleRate", req.getInputSampleRate());
		json.add("frequency", req.getSatelliteFrequency());
		json.add("decoder", req.getDecoder());
		json.add("satellite", req.getSatelliteId());

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
			json.add("aURL", result.getaURL());
		}
		if (result.getDataURL() != null) {
			json.add("data", result.getDataURL());
		}
		if (result.getSpectogramURL() != null) {
			json.add("spectogramURL", result.getSpectogramURL());
		}
		return json;
	}

}

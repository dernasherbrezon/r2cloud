package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class LoraModulation {

	private long loraBandwidth;
	private int loraSpreadFactor;
	private int loraSyncword;
	private int loraPreambleLength;
	private int loraLdro;
	private boolean loraExplicitHeader;
	// should be configured only for implicit mode header.
	private int loraCodingRate;
	private boolean loraCrc;
	private int beaconSizeBytes;

	public LoraModulation() {
		// do nothing
	}

	public LoraModulation(LoraModulation copy) {
		this.loraBandwidth = copy.loraBandwidth;
		this.loraSpreadFactor = copy.loraSpreadFactor;
		this.loraCodingRate = copy.loraCodingRate;
		this.loraSyncword = copy.loraSyncword;
		this.loraPreambleLength = copy.loraPreambleLength;
		this.loraLdro = copy.loraLdro;
		this.loraExplicitHeader = copy.loraExplicitHeader;
		this.loraCrc = copy.loraCrc;
		this.beaconSizeBytes = copy.beaconSizeBytes;
	}

	public long getLoraBandwidth() {
		return loraBandwidth;
	}

	public void setLoraBandwidth(long loraBandwidth) {
		this.loraBandwidth = loraBandwidth;
	}

	public int getLoraSpreadFactor() {
		return loraSpreadFactor;
	}

	public void setLoraSpreadFactor(int loraSpreadFactor) {
		this.loraSpreadFactor = loraSpreadFactor;
	}

	public int getLoraSyncword() {
		return loraSyncword;
	}

	public void setLoraSyncword(int loraSyncword) {
		this.loraSyncword = loraSyncword;
	}

	public int getLoraPreambleLength() {
		return loraPreambleLength;
	}

	public void setLoraPreambleLength(int loraPreambleLength) {
		this.loraPreambleLength = loraPreambleLength;
	}

	public int getLoraLdro() {
		return loraLdro;
	}

	public void setLoraLdro(int loraLdro) {
		this.loraLdro = loraLdro;
	}

	public boolean isLoraExplicitHeader() {
		return loraExplicitHeader;
	}

	public void setLoraExplicitHeader(boolean loraExplicitHeader) {
		this.loraExplicitHeader = loraExplicitHeader;
	}

	public int getLoraCodingRate() {
		return loraCodingRate;
	}

	public void setLoraCodingRate(int loraCodingRate) {
		this.loraCodingRate = loraCodingRate;
	}

	public boolean isLoraCrc() {
		return loraCrc;
	}

	public void setLoraCrc(boolean loraCrc) {
		this.loraCrc = loraCrc;
	}

	public int getBeaconSizeBytes() {
		return beaconSizeBytes;
	}

	public void setBeaconSizeBytes(int beaconSizeBytes) {
		this.beaconSizeBytes = beaconSizeBytes;
	}

	public static LoraModulation fromJson(JsonObject asObject) {
		LoraModulation result = new LoraModulation();
		JsonValue loraBandwidth = asObject.get("loraBandwidth");
		if (loraBandwidth != null) {
			result.setLoraBandwidth(loraBandwidth.asLong());
		}
		JsonValue loraSpreadFactor = asObject.get("loraSpreadFactor");
		if (loraSpreadFactor != null) {
			result.setLoraSpreadFactor(loraSpreadFactor.asInt());
		}
		JsonValue loraCodingRate = asObject.get("loraCodingRate");
		if (loraCodingRate != null) {
			result.setLoraCodingRate(loraCodingRate.asInt());
		}
		JsonValue loraSyncword = asObject.get("loraSyncword");
		if (loraSyncword != null) {
			result.setLoraSyncword(loraSyncword.asInt());
		}
		JsonValue loraPreambleLength = asObject.get("loraPreambleLength");
		if (loraPreambleLength != null) {
			result.setLoraPreambleLength(loraPreambleLength.asInt());
		}
		JsonValue loraLdro = asObject.get("loraLdro");
		if (loraLdro != null) {
			result.setLoraLdro(loraLdro.asInt());
		}
		JsonValue loraCrc = asObject.get("loraCrc");
		if (loraCrc != null) {
			result.setLoraCrc(loraCrc.asBoolean());
		} else {
			result.setLoraCrc(true);
		}
		JsonValue loraExplicitHeader = asObject.get("loraExplicitHeader");
		if (loraExplicitHeader != null) {
			result.setLoraExplicitHeader(loraExplicitHeader.asBoolean());
		} else {
			result.setLoraExplicitHeader(true);
		}
		JsonValue beaconSizeBytes = asObject.get("beaconSizeBytes");
		if (beaconSizeBytes != null) {
			result.setBeaconSizeBytes(beaconSizeBytes.asInt());
		}
		return result;
	}

	// make compatible with Transmitter json and be on the same level
	public void toJson(JsonObject result) {
		if (loraBandwidth != 0) {
			result.add("loraBandwidth", loraBandwidth);
		}
		if (loraSpreadFactor != 0) {
			result.add("loraSpreadFactor", loraSpreadFactor);
		}
		if (loraCodingRate != 0) {
			result.add("loraCodingRate", loraCodingRate);
		}
		if (loraSyncword != 0) {
			result.add("loraSyncword", loraSyncword);
		}
		if (loraPreambleLength != 0) {
			result.add("loraPreambleLength", loraPreambleLength);
		}
		if (loraLdro != 0) {
			result.add("loraLdro", loraLdro);
		}
		result.add("loraExplicitHeader", loraExplicitHeader);
		result.add("loraCrc", loraCrc);
		if (beaconSizeBytes != 0) {
			result.add("beaconSizeBytes", beaconSizeBytes);
		}
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		toJson(result);
		return result;
	}

}

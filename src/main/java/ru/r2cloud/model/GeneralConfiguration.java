package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class GeneralConfiguration {

	private boolean locationAuto;
	private Double lat;
	private Double lng;
	private Double alt;
	private boolean autoUpdate;
	private boolean presentationMode;
	private Integer retentionRawCount;
	private Long retentionMaxSizeBytes;

	public boolean isLocationAuto() {
		return locationAuto;
	}

	public void setLocationAuto(boolean locationAuto) {
		this.locationAuto = locationAuto;
	}

	public Long getRetentionMaxSizeBytes() {
		return retentionMaxSizeBytes;
	}

	public void setRetentionMaxSizeBytes(Long retentionMaxSizeBytes) {
		this.retentionMaxSizeBytes = retentionMaxSizeBytes;
	}

	public Integer getRetentionRawCount() {
		return retentionRawCount;
	}

	public void setRetentionRawCount(Integer retentionRawCount) {
		this.retentionRawCount = retentionRawCount;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

	public Double getAlt() {
		return alt;
	}

	public void setAlt(Double alt) {
		this.alt = alt;
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}

	public boolean isPresentationMode() {
		return presentationMode;
	}

	public void setPresentationMode(boolean presentationMode) {
		this.presentationMode = presentationMode;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.add("locationAuto", locationAuto);
		if (lat != null) {
			result.add("lat", lat);
		}
		if (lng != null) {
			result.add("lng", lng);
		}
		if (alt != null) {
			result.add("alt", alt);
		}
		result.add("autoUpdate", autoUpdate);
		result.add("presentationMode", presentationMode);
		result.add("retentionRawCount", retentionRawCount);
		result.add("retentionMaxSizeBytes", retentionMaxSizeBytes);
		return result;
	}

	public static GeneralConfiguration fromJson(JsonObject meta) {
		GeneralConfiguration result = new GeneralConfiguration();
		result.setLocationAuto(meta.getBoolean("locationAuto", false));
		JsonValue lat = meta.get("lat");
		if (lat != null) {
			result.setLat(lat.asDouble());
		}
		JsonValue lng = meta.get("lng");
		if (lng != null) {
			result.setLng(lng.asDouble());
		}
		JsonValue alt = meta.get("alt");
		if (alt != null) {
			result.setAlt(alt.asDouble());
		}
		result.setAutoUpdate(meta.getBoolean("autoUpdate", false));
		result.setPresentationMode(meta.getBoolean("presentationMode", false));
		JsonValue retentionRawCount = meta.get("retentionRawCount");
		if (retentionRawCount != null) {
			result.setRetentionRawCount(retentionRawCount.asInt());
		}
		JsonValue retentionMaxSizeBytes = meta.get("retentionMaxSizeBytes");
		if (retentionMaxSizeBytes != null) {
			result.setRetentionMaxSizeBytes(retentionMaxSizeBytes.asLong());
		}
		return result;
	}

}

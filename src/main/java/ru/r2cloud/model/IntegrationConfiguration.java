package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class IntegrationConfiguration {

	private String apiKey;
	private boolean syncSpectogram;
	private boolean newLaunch;
	private boolean satnogs;
	private String influxdbUsername;
	private String influxdbPassword;
	private String influxdbDatabase;
	private String influxdbHostname;
	private Integer influxdbPort;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public boolean isSyncSpectogram() {
		return syncSpectogram;
	}

	public void setSyncSpectogram(boolean syncSpectogram) {
		this.syncSpectogram = syncSpectogram;
	}

	public boolean isNewLaunch() {
		return newLaunch;
	}

	public void setNewLaunch(boolean newLaunch) {
		this.newLaunch = newLaunch;
	}

	public boolean isSatnogs() {
		return satnogs;
	}

	public void setSatnogs(boolean satnogs) {
		this.satnogs = satnogs;
	}

	public String getInfluxdbUsername() {
		return influxdbUsername;
	}

	public void setInfluxdbUsername(String influxdbUsername) {
		this.influxdbUsername = influxdbUsername;
	}

	public String getInfluxdbPassword() {
		return influxdbPassword;
	}

	public void setInfluxdbPassword(String influxdbPassword) {
		this.influxdbPassword = influxdbPassword;
	}

	public String getInfluxdbDatabase() {
		return influxdbDatabase;
	}

	public void setInfluxdbDatabase(String influxdbDatabase) {
		this.influxdbDatabase = influxdbDatabase;
	}

	public String getInfluxdbHostname() {
		return influxdbHostname;
	}

	public void setInfluxdbHostname(String influxdbHostname) {
		this.influxdbHostname = influxdbHostname;
	}

	public Integer getInfluxdbPort() {
		return influxdbPort;
	}

	public void setInfluxdbPort(Integer influxdbPort) {
		this.influxdbPort = influxdbPort;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.add("apiKey", apiKey);
		result.add("syncSpectogram", syncSpectogram);
		result.add("newLaunch", newLaunch);
		result.add("satnogs", satnogs);
		if (influxdbHostname != null) {
			result.add("influxdbHostname", influxdbHostname);
		}
		if (influxdbPort != null) {
			result.add("influxdbPort", influxdbPort);
		}
		if (influxdbUsername != null) {
			result.add("influxdbUsername", influxdbUsername);
		}
		if (influxdbPassword != null) {
			result.add("influxdbPassword", influxdbPassword);
		}
		if (influxdbDatabase != null) {
			result.add("influxdbDatabase", influxdbDatabase);
		}
		return result;
	}

	public static IntegrationConfiguration fromJson(JsonObject meta) {
		IntegrationConfiguration result = new IntegrationConfiguration();
		result.setApiKey(meta.getString("apiKey", null));
		result.setSyncSpectogram(meta.getBoolean("syncSpectogram", false));
		result.setNewLaunch(meta.getBoolean("newLaunch", false));
		result.setSatnogs(meta.getBoolean("satnogs", false));
		result.setInfluxdbHostname(meta.getString("influxdbHostname", null));
		JsonValue port = meta.get("influxdbPort");
		if (port != null) {
			result.setInfluxdbPort(port.asInt());
		}
		result.setInfluxdbUsername(meta.getString("influxdbUsername", null));
		result.setInfluxdbPassword(meta.getString("influxdbPassword", null));
		result.setInfluxdbDatabase(meta.getString("influxdbDatabase", null));
		return result;
	}
}

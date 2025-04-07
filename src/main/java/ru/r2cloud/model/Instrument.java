package ru.r2cloud.model;

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Instrument {

	private String id;
	private String name;
	private String satdumpName;
	private String satdumpCombined;
	private String description;
	private List<InstrumentChannel> channels;
	private boolean enabled;
	private boolean primary;

	public Instrument() {
		// do nothing
	}

	public Instrument(Instrument other) {
		this.id = other.id;
		this.name = other.name;
		this.satdumpName = other.satdumpName;
		this.satdumpCombined = other.satdumpCombined;
		this.description = other.description;
		if (other.channels != null) {
			this.channels = new ArrayList<>(other.channels.size());
			for (InstrumentChannel cur : other.channels) {
				this.channels.add(new InstrumentChannel(cur));
			}
		}
		this.enabled = other.enabled;
		this.primary = other.primary;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSatdumpName() {
		return satdumpName;
	}

	public void setSatdumpName(String satdumpName) {
		this.satdumpName = satdumpName;
	}

	public String getSatdumpCombined() {
		return satdumpCombined;
	}

	public void setSatdumpCombined(String satdumpCombined) {
		this.satdumpCombined = satdumpCombined;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<InstrumentChannel> getChannels() {
		return channels;
	}

	public void setChannels(List<InstrumentChannel> channels) {
		this.channels = channels;
	}

	public static Instrument fromJson(JsonObject obj) {
		Instrument result = new Instrument();
		result.setId(obj.getString("id", null));
		if (result.getId() == null) {
			return null;
		}
		result.setEnabled(obj.getBoolean("enabled", false));
		result.setPrimary(obj.getBoolean("primary", false));
		result.setName(obj.getString("name", null));
		result.setSatdumpName(obj.getString("satdumpName", null));
		result.setSatdumpCombined(obj.getString("satdumpCombined", null));
		result.setDescription(obj.getString("description", null));
		JsonValue channels = obj.get("channels");
		if (channels != null && channels.isArray()) {
			List<InstrumentChannel> instrumentChannels = new ArrayList<>();
			JsonArray channelsArray = channels.asArray();
			for (int i = 0; i < channelsArray.size(); i++) {
				InstrumentChannel cur = InstrumentChannel.fromJson(channelsArray.get(i).asObject());
				if (cur == null) {
					continue;
				}
				instrumentChannels.add(cur);
			}
			result.setChannels(instrumentChannels);
		}
		return result;
	}

}

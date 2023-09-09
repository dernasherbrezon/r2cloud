package ru.r2cloud.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Satellite {

	private String id;
	private String name;
	// enable to automatic scheduling, otherwise have to be enabled manually
	private boolean enabled;
	private Tle tle;
	private Priority priority;
	// active period
	// used for new launches when noradid not yet defined
	private Date start;
	private Date end;
	private List<Transmitter> transmitters;
	private SatelliteSource source;

	public SatelliteSource getSource() {
		return source;
	}

	public void setSource(SatelliteSource source) {
		this.source = source;
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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (transmitters != null) {
			for (Transmitter cur : transmitters) {
				cur.setEnabled(enabled);
			}
		}
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
		if (transmitters != null) {
			for (Transmitter cur : transmitters) {
				cur.setTle(tle);
			}
		}
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public List<Transmitter> getTransmitters() {
		return transmitters;
	}

	public Transmitter getFirstOrNull() {
		if (transmitters.isEmpty()) {
			return null;
		}
		return transmitters.get(0);
	}

	public void setTransmitters(List<Transmitter> transmitters) {
		this.transmitters = transmitters;
	}

	public Transmitter getById(String id) {
		if (id == null) {
			// support for legacy observations
			// select first transmitter
			if (transmitters.size() > 0) {
				return transmitters.get(0);
			}
			return null;
		}
		for (Transmitter cur : transmitters) {
			if (cur.getId().equalsIgnoreCase(id)) {
				return cur;
			}
		}
		return null;
	}

	public TransmitterStatus getOverallStatus() {
		TransmitterStatus result = null;
		for (Transmitter cur : transmitters) {
			// at least 1 ENABLED is required for overall ENABLED
			if (cur.getStatus().equals(TransmitterStatus.ENABLED)) {
				return cur.getStatus();
			}
			result = cur.getStatus();
		}
		return result;
	}

	@Override
	public String toString() {
		return name + "(" + id + ")";
	}

	public static Satellite fromJson(JsonObject meta) {
		Satellite result = new Satellite();
		result.setId(meta.getString("noradId", null));
		result.setName(meta.getString("name", null));
		String priorityStr = meta.getString("priority", null);
		if (priorityStr != null) {
			try {
				result.setPriority(Priority.valueOf(priorityStr));
			} catch (IllegalArgumentException e) {
				return null;
			}
		} else {
			result.setPriority(Priority.NORMAL);
		}
		result.setEnabled(meta.getBoolean("enabled", false));
		long startTimeMillis = meta.getLong("start", 0);
		if (startTimeMillis != 0) {
			result.setStart(new Date(startTimeMillis));
		}
		long endTimeMillis = meta.getLong("end", 0);
		if (endTimeMillis != 0) {
			result.setEnd(new Date(endTimeMillis));
		}
		List<Transmitter> transmitters = new ArrayList<>();
		result.setTransmitters(transmitters);
		JsonValue transmittersRaw = meta.get("transmitters");
		if (transmittersRaw != null && transmittersRaw.isArray()) {
			JsonArray transmittersArray = transmittersRaw.asArray();
			for (int i = 0; i < transmittersArray.size(); i++) {
				Transmitter cur = Transmitter.fromJson(transmittersArray.get(i).asObject());
				if (cur == null) {
					continue;
				}
				transmitters.add(cur);
			}
		}
		if (transmitters.isEmpty()) {
			return null;
		}
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.add("name", name);
		result.add("noradId", id);
		result.add("priority", priority.name());
		result.add("enabled", enabled);
		if (start != null) {
			result.add("start", start.getTime());
		}
		if (end != null) {
			result.add("end", end.getTime());
		}
		JsonArray transmittersJson = new JsonArray();
		for (Transmitter cur : transmitters) {
			transmittersJson.add(cur.toJson());
		}
		result.add("transmitters", transmittersJson);
		return result;
	}

}

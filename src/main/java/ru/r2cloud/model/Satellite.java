package ru.r2cloud.model;

import java.util.Date;
import java.util.List;

public class Satellite {

	private String id;
	private String name;
	private boolean enabled;
	private Tle tle;
	private Priority priority;
	// active period
	// used for new launches when noradid not yet defined
	private Date start;
	private Date end;
	private List<Transmitter> transmitters;

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
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
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

	public void setTransmitters(List<Transmitter> transmitters) {
		this.transmitters = transmitters;
	}

	public Transmitter getById(String id) {
		for (Transmitter cur : transmitters) {
			if (cur.getId().equalsIgnoreCase(id)) {
				return cur;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return name + "(" + id + ")";
	}

}

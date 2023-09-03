package ru.r2cloud.model;

import org.orekit.bodies.GeodeticPoint;

public class ObservationRequest {

	private String id;
	private long startTimeMillis;
	private long endTimeMillis;
	private String satelliteId;
	private String transmitterId;
	private Tle tle;
	private GeodeticPoint groundStation;
	private long frequency;
	private long centerBandFrequency;
	
	public long getCenterBandFrequency() {
		return centerBandFrequency;
	}
	
	public void setCenterBandFrequency(long centerBandFrequency) {
		this.centerBandFrequency = centerBandFrequency;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public long getEndTimeMillis() {
		return endTimeMillis;
	}

	public void setEndTimeMillis(long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public String getTransmitterId() {
		return transmitterId;
	}

	public void setTransmitterId(String transmitterId) {
		this.transmitterId = transmitterId;
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
	}

	public GeodeticPoint getGroundStation() {
		return groundStation;
	}

	public void setGroundStation(GeodeticPoint groundStation) {
		this.groundStation = groundStation;
	}

	public long getFrequency() {
		return frequency;
	}
	
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

}

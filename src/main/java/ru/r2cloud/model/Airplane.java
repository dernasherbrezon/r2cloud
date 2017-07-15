package ru.r2cloud.model;

import java.util.List;

import org.opensky.libadsb.Position;
import org.opensky.libadsb.PositionDecoder;

public class Airplane {

	private String icao24;
	private List<Position> positions;

	private transient long lastUpdatedAt;
	private transient PositionDecoder decoder;

	public long getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public void setLastUpdatedAt(long lastUpdatedAt) {
		this.lastUpdatedAt = lastUpdatedAt;
	}

	public String getIcao24() {
		return icao24;
	}

	public void setIcao24(String icao24) {
		this.icao24 = icao24;
	}

	public List<Position> getPositions() {
		return positions;
	}

	public void setPositions(List<Position> positions) {
		this.positions = positions;
	}

	public PositionDecoder getDecoder() {
		return decoder;
	}

	public void setDecoder(PositionDecoder decoder) {
		this.decoder = decoder;
	}

}

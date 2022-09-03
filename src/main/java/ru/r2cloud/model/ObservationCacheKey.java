package ru.r2cloud.model;

public class ObservationCacheKey {

	private String satelliteId;
	private String observationId;

	public ObservationCacheKey(String satelliteId, String observationId) {
		super();
		this.satelliteId = satelliteId;
		this.observationId = observationId;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public String getObservationId() {
		return observationId;
	}

	public void setObservationId(String observationId) {
		this.observationId = observationId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((observationId == null) ? 0 : observationId.hashCode());
		result = prime * result + ((satelliteId == null) ? 0 : satelliteId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObservationCacheKey other = (ObservationCacheKey) obj;
		if (observationId == null) {
			if (other.observationId != null)
				return false;
		} else if (!observationId.equals(other.observationId))
			return false;
		if (satelliteId == null) {
			if (other.satelliteId != null)
				return false;
		} else if (!satelliteId.equals(other.satelliteId))
			return false;
		return true;
	}

}

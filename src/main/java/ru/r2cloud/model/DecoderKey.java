package ru.r2cloud.model;

public class DecoderKey {

	private String satelliteId;
	private String transmitterId;

	public DecoderKey(String satelliteId, String transmitterId) {
		this.satelliteId = satelliteId;
		this.transmitterId = transmitterId;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((satelliteId == null) ? 0 : satelliteId.hashCode());
		result = prime * result + ((transmitterId == null) ? 0 : transmitterId.hashCode());
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
		DecoderKey other = (DecoderKey) obj;
		if (satelliteId == null) {
			if (other.satelliteId != null)
				return false;
		} else if (!satelliteId.equals(other.satelliteId))
			return false;
		if (transmitterId == null) {
			if (other.transmitterId != null)
				return false;
		} else if (!transmitterId.equals(other.transmitterId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DecoderKey [satelliteId=" + satelliteId + ", transmitterId=" + transmitterId + "]";
	}

}

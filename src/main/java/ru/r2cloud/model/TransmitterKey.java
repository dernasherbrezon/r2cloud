package ru.r2cloud.model;

public class TransmitterKey {

	private String satelliteId;
	private Modulation modulation;
	private Framing framing;
	private long frequency;

	public TransmitterKey(Transmitter cur) {
		satelliteId = cur.getSatelliteId();
		modulation = cur.getModulation();
		framing = cur.getFraming();
		frequency = cur.getFrequency();
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public Modulation getModulation() {
		return modulation;
	}

	public void setModulation(Modulation modulation) {
		this.modulation = modulation;
	}

	public Framing getFraming() {
		return framing;
	}

	public void setFraming(Framing framing) {
		this.framing = framing;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((framing == null) ? 0 : framing.hashCode());
		result = prime * result + (int) (frequency ^ (frequency >>> 32));
		result = prime * result + ((modulation == null) ? 0 : modulation.hashCode());
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
		TransmitterKey other = (TransmitterKey) obj;
		if (framing != other.framing)
			return false;
		if (frequency != other.frequency)
			return false;
		if (modulation != other.modulation)
			return false;
		if (satelliteId == null) {
			if (other.satelliteId != null)
				return false;
		} else if (!satelliteId.equals(other.satelliteId))
			return false;
		return true;
	}

}

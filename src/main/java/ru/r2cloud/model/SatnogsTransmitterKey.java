package ru.r2cloud.model;

import java.util.ArrayList;
import java.util.List;

public class SatnogsTransmitterKey {

	private Modulation modulation;
	private Framing framing;
	private List<Integer> baudRates;

	public SatnogsTransmitterKey(Transmitter cur) {
		this.modulation = cur.getModulation();
		this.framing = cur.getFraming();
		this.baudRates = new ArrayList<>(cur.getBaudRates());
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

	public List<Integer> getBaudRates() {
		return baudRates;
	}

	public void setBaudRates(List<Integer> baudRates) {
		this.baudRates = baudRates;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baudRates == null) ? 0 : baudRates.hashCode());
		result = prime * result + ((framing == null) ? 0 : framing.hashCode());
		result = prime * result + ((modulation == null) ? 0 : modulation.hashCode());
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
		SatnogsTransmitterKey other = (SatnogsTransmitterKey) obj;
		if (baudRates == null) {
			if (other.baudRates != null)
				return false;
		} else if (!baudRates.equals(other.baudRates))
			return false;
		if (framing != other.framing)
			return false;
		if (modulation != other.modulation)
			return false;
		return true;
	}

}

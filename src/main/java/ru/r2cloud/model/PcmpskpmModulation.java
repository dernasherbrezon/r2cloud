package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;

public class PcmpskpmModulation {

	private long subcarrierFrequency;
	private float modulationIndex;
	private float carrierPllBandwidth;

	// bpsk settings
	private float rrcBeta;
	private int rrcDelay;
	private float costasBandwidth;
	private int filterBankSize;

	public PcmpskpmModulation() {
		// do nothing
	}

	public PcmpskpmModulation(PcmpskpmModulation other) {
		this.subcarrierFrequency = other.subcarrierFrequency;
		this.modulationIndex = other.modulationIndex;
		this.carrierPllBandwidth = other.carrierPllBandwidth;
		this.rrcBeta = other.rrcBeta;
		this.rrcDelay = other.rrcDelay;
		this.costasBandwidth = other.costasBandwidth;
		this.filterBankSize = other.filterBankSize;
	}

	public long getSubcarrierFrequency() {
		return subcarrierFrequency;
	}

	public void setSubcarrierFrequency(long subcarrierFrequency) {
		this.subcarrierFrequency = subcarrierFrequency;
	}

	public float getModulationIndex() {
		return modulationIndex;
	}

	public void setModulationIndex(float modulationIndex) {
		this.modulationIndex = modulationIndex;
	}

	public float getCarrierPllBandwidth() {
		return carrierPllBandwidth;
	}

	public void setCarrierPllBandwidth(float carrierPllBandwidth) {
		this.carrierPllBandwidth = carrierPllBandwidth;
	}

	public float getRrcBeta() {
		return rrcBeta;
	}

	public void setRrcBeta(float rrcBeta) {
		this.rrcBeta = rrcBeta;
	}

	public int getRrcDelay() {
		return rrcDelay;
	}

	public void setRrcDelay(int rrcDelay) {
		this.rrcDelay = rrcDelay;
	}

	public float getCostasBandwidth() {
		return costasBandwidth;
	}

	public void setCostasBandwidth(float costasBandwidth) {
		this.costasBandwidth = costasBandwidth;
	}

	public int getFilterBankSize() {
		return filterBankSize;
	}

	public void setFilterBankSize(int filterBankSize) {
		this.filterBankSize = filterBankSize;
	}

	public static PcmpskpmModulation fromJson(JsonObject obj) {
		PcmpskpmModulation result = new PcmpskpmModulation();
		result.setCarrierPllBandwidth(obj.getLong("subcarrierFrequency", 0));
		result.setModulationIndex(obj.getFloat("modulationIndex", 0));
		result.setCarrierPllBandwidth(obj.getFloat("carrierPllBandwidth", 0));

		result.setRrcBeta(obj.getFloat("rrcBeta", 0));
		result.setRrcDelay(obj.getInt("rrcDelay", 0));
		result.setCostasBandwidth(obj.getFloat("costasBandwidth", 0));
		result.setFilterBankSize(obj.getInt("filterBankSize", 0));
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.set("subcarrierFrequency", subcarrierFrequency);
		result.set("modulationIndex", modulationIndex);
		result.set("carrierPllBandwidth", carrierPllBandwidth);

		result.set("rrcBeta", rrcBeta);
		result.set("rrcDelay", rrcDelay);
		result.set("costasBandwidth", costasBandwidth);
		result.set("filterBankSize", filterBankSize);
		return result;
	}

}

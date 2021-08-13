package ru.r2cloud.model;

import java.util.List;

import ru.r2cloud.jradio.Beacon;

public class Satellite {

	private String id;
	private String name;
	private FrequencySource source;
	private long frequency;
	private boolean enabled;
	private long bandwidth;
	private List<Integer> baudRates;
	private BandFrequency frequencyBand;
	private int inputSampleRate;
	private int outputSampleRate;
	private Modulation modulation;
	private Framing framing;
	private Class<? extends Beacon> beaconClass;
	
	public Class<? extends Beacon> getBeaconClass() {
		return beaconClass;
	}
	
	public void setBeaconClass(Class<? extends Beacon> beaconClass) {
		this.beaconClass = beaconClass;
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
	
	public int getInputSampleRate() {
		return inputSampleRate;
	}
	
	public void setInputSampleRate(int inputSampleRate) {
		this.inputSampleRate = inputSampleRate;
	}
	
	public int getOutputSampleRate() {
		return outputSampleRate;
	}
	
	public void setOutputSampleRate(int outputSampleRate) {
		this.outputSampleRate = outputSampleRate;
	}
	
	public BandFrequency getFrequencyBand() {
		return frequencyBand;
	}
	
	public void setFrequencyBand(BandFrequency frequencyBand) {
		this.frequencyBand = frequencyBand;
	}
	
	public List<Integer> getBaudRates() {
		return baudRates;
	}
	
	public void setBaudRates(List<Integer> baudRates) {
		this.baudRates = baudRates;
	}

	public long getBandwidth() {
		return bandwidth;
	}
	
	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public FrequencySource getSource() {
		return source;
	}
	
	public void setSource(FrequencySource source) {
		this.source = source;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
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

	@Override
	public String toString() {
		return name + "(" + id + ")";
	}
}

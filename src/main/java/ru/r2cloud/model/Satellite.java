package ru.r2cloud.model;

public class Satellite {

	private String id;
	private String name;
	private FrequencySource source;
	private long frequency;
	private boolean enabled;
	private long bandwidth;
	private Integer baud;
	private BandFrequency frequencyBand;
	
	public BandFrequency getFrequencyBand() {
		return frequencyBand;
	}
	
	public void setFrequencyBand(BandFrequency frequencyBand) {
		this.frequencyBand = frequencyBand;
	}

	public Integer getBaud() {
		return baud;
	}
	
	public void setBaud(Integer baud) {
		this.baud = baud;
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

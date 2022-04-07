package ru.r2cloud.model;

import java.util.Date;
import java.util.List;

import ru.r2cloud.jradio.Beacon;

public class Transmitter {

	private String id;
	private Modulation modulation;
	private Framing framing;
	private Class<? extends Beacon> beaconClass;
	private int beaconSizeBytes;
	private long frequency;
	private BandFrequency frequencyBand;
	private long bandwidth;
	private List<Integer> baudRates;
	private byte[] assistedHeader;
	private boolean bpskDifferential;
	private double bpskCenterFrequency;
	private long deviation;
	private long afCarrier;
	private double transitionWidth;
	private int inputSampleRate;
	private int outputSampleRate;

	private long loraBandwidth;
	private int loraSpreadFactor;
	private int loraCodingRate;
	private int loraSyncword;
	private int loraPreambleLength;
	private int loraLdro;

	// copied from satellite
	// to simplify code
	private boolean enabled;
	private String satelliteId;
	private Priority priority;
	private Date start;
	private Date end;

	public Transmitter() {
		// do nothing
	}

	public Transmitter(Transmitter copy) {
		this.id = copy.id;
		this.enabled = copy.enabled;
		this.satelliteId = copy.satelliteId;
		this.start = copy.start;
		this.end = copy.end;
		this.priority = copy.priority;
		this.modulation = copy.modulation;
		this.framing = copy.framing;
		this.beaconClass = copy.beaconClass;
		this.beaconSizeBytes = copy.beaconSizeBytes;
		this.frequency = copy.frequency;
		this.frequencyBand = copy.frequencyBand;
		this.bandwidth = copy.bandwidth;
		this.baudRates = copy.baudRates;
		this.assistedHeader = copy.assistedHeader;
		this.bpskDifferential = copy.bpskDifferential;
		this.bpskCenterFrequency = copy.bpskCenterFrequency;
		this.deviation = copy.deviation;
		this.afCarrier = copy.afCarrier;
		this.transitionWidth = copy.transitionWidth;
		this.inputSampleRate = copy.inputSampleRate;
		this.outputSampleRate = copy.outputSampleRate;
		this.loraBandwidth = copy.loraBandwidth;
		this.loraSpreadFactor = copy.loraSpreadFactor;
		this.loraCodingRate = copy.loraCodingRate;
		this.loraSyncword = copy.loraSyncword;
		this.loraPreambleLength = copy.loraPreambleLength;
		this.loraLdro = copy.loraLdro;
	}
	
	public double getTransitionWidth() {
		return transitionWidth;
	}
	
	public void setTransitionWidth(double transitionWidth) {
		this.transitionWidth = transitionWidth;
	}
	
	public long getAfCarrier() {
		return afCarrier;
	}
	
	public void setAfCarrier(long afCarrier) {
		this.afCarrier = afCarrier;
	}
	
	public long getDeviation() {
		return deviation;
	}
	
	public void setDeviation(long deviation) {
		this.deviation = deviation;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
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

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Class<? extends Beacon> getBeaconClass() {
		return beaconClass;
	}

	public void setBeaconClass(Class<? extends Beacon> beaconClass) {
		this.beaconClass = beaconClass;
	}

	public int getBeaconSizeBytes() {
		return beaconSizeBytes;
	}

	public void setBeaconSizeBytes(int beaconSizeBytes) {
		this.beaconSizeBytes = beaconSizeBytes;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public BandFrequency getFrequencyBand() {
		return frequencyBand;
	}

	public void setFrequencyBand(BandFrequency frequencyBand) {
		this.frequencyBand = frequencyBand;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public List<Integer> getBaudRates() {
		return baudRates;
	}

	public void setBaudRates(List<Integer> baudRates) {
		this.baudRates = baudRates;
	}

	public byte[] getAssistedHeader() {
		return assistedHeader;
	}

	public void setAssistedHeader(byte[] assistedHeader) {
		this.assistedHeader = assistedHeader;
	}

	public boolean isBpskDifferential() {
		return bpskDifferential;
	}

	public void setBpskDifferential(boolean bpskDifferential) {
		this.bpskDifferential = bpskDifferential;
	}

	public double getBpskCenterFrequency() {
		return bpskCenterFrequency;
	}

	public void setBpskCenterFrequency(double bpskCenterFrequency) {
		this.bpskCenterFrequency = bpskCenterFrequency;
	}

	public long getLoraBandwidth() {
		return loraBandwidth;
	}

	public void setLoraBandwidth(long loraBandwidth) {
		this.loraBandwidth = loraBandwidth;
	}

	public int getLoraSpreadFactor() {
		return loraSpreadFactor;
	}

	public void setLoraSpreadFactor(int loraSpreadFactor) {
		this.loraSpreadFactor = loraSpreadFactor;
	}

	public int getLoraCodingRate() {
		return loraCodingRate;
	}

	public void setLoraCodingRate(int loraCodingRate) {
		this.loraCodingRate = loraCodingRate;
	}

	public int getLoraSyncword() {
		return loraSyncword;
	}

	public void setLoraSyncword(int loraSyncword) {
		this.loraSyncword = loraSyncword;
	}

	public int getLoraPreambleLength() {
		return loraPreambleLength;
	}

	public void setLoraPreambleLength(int loraPreambleLength) {
		this.loraPreambleLength = loraPreambleLength;
	}

	public int getLoraLdro() {
		return loraLdro;
	}

	public void setLoraLdro(int loraLdro) {
		this.loraLdro = loraLdro;
	}
	
	@Override
	public String toString() {
		return id;
	}

}

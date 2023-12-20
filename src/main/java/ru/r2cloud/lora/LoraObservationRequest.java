package ru.r2cloud.lora;

public class LoraObservationRequest {

	private long frequency;
	private long bw;
	private int sf;
	private int cr;
	private int syncword;
	private int preambleLength;
	private int gain;
	private int ldro;
	private boolean useCrc;
	private boolean useExplicitHeader;
	private int beaconSizeBytes;
	
	public int getBeaconSizeBytes() {
		return beaconSizeBytes;
	}
	
	public void setBeaconSizeBytes(int beaconSizeBytes) {
		this.beaconSizeBytes = beaconSizeBytes;
	}

	public boolean isUseCrc() {
		return useCrc;
	}

	public void setUseCrc(boolean useCrc) {
		this.useCrc = useCrc;
	}

	public boolean isUseExplicitHeader() {
		return useExplicitHeader;
	}

	public void setUseExplicitHeader(boolean useExplicitHeader) {
		this.useExplicitHeader = useExplicitHeader;
	}

	public long getFrequency() {
		return frequency;
	}
	
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public long getBw() {
		return bw;
	}
	
	public void setBw(long bw) {
		this.bw = bw;
	}
	
	public int getSf() {
		return sf;
	}

	public void setSf(int sf) {
		this.sf = sf;
	}

	public int getCr() {
		return cr;
	}

	public void setCr(int cr) {
		this.cr = cr;
	}

	public int getSyncword() {
		return syncword;
	}

	public void setSyncword(int syncword) {
		this.syncword = syncword;
	}

	public int getPreambleLength() {
		return preambleLength;
	}

	public void setPreambleLength(int preambleLength) {
		this.preambleLength = preambleLength;
	}

	public int getGain() {
		return gain;
	}

	public void setGain(int gain) {
		this.gain = gain;
	}

	public int getLdro() {
		return ldro;
	}

	public void setLdro(int ldro) {
		this.ldro = ldro;
	}

}

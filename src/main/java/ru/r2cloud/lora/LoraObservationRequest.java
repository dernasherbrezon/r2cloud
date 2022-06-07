package ru.r2cloud.lora;

public class LoraObservationRequest {

	private float frequency;
	private float bw;
	private int sf;
	private int cr;
	private int syncword;
	private int preambleLength;
	private int gain;
	private int ldro;

	public float getFrequency() {
		return frequency;
	}

	public void setFrequency(float frequency) {
		this.frequency = frequency;
	}

	public float getBw() {
		return bw;
	}

	public void setBw(float bw) {
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

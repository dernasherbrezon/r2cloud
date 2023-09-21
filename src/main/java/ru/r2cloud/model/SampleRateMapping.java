package ru.r2cloud.model;

public class SampleRateMapping {

	private long deviceOutput;
	private long demodulatorInput;
	private long symbolSyncInput;
	private int baudRate;

	public SampleRateMapping() {
		// do nothing
	}

	public SampleRateMapping(long deviceOutput, long demodulatorInput, long symbolSyncInput, int baudRate) {
		super();
		this.deviceOutput = deviceOutput;
		this.demodulatorInput = demodulatorInput;
		this.symbolSyncInput = symbolSyncInput;
		this.baudRate = baudRate;
	}

	public long getDeviceOutput() {
		return deviceOutput;
	}

	public void setDeviceOutput(long deviceOutput) {
		this.deviceOutput = deviceOutput;
	}

	public long getDemodulatorInput() {
		return demodulatorInput;
	}

	public void setDemodulatorInput(long demodulatorInput) {
		this.demodulatorInput = demodulatorInput;
	}

	public long getSymbolSyncInput() {
		return symbolSyncInput;
	}

	public void setSymbolSyncInput(long symbolSyncInput) {
		this.symbolSyncInput = symbolSyncInput;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

}

package ru.r2cloud.model;

public class SampleRateKey {

	private long deviceOutput;
	private int baudRate;

	public SampleRateKey(long deviceOutput, int baudRate) {
		super();
		this.deviceOutput = deviceOutput;
		this.baudRate = baudRate;
	}

	public long getDeviceOutput() {
		return deviceOutput;
	}

	public void setDeviceOutput(long deviceOutput) {
		this.deviceOutput = deviceOutput;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + baudRate;
		result = prime * result + (int) (deviceOutput ^ (deviceOutput >>> 32));
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
		SampleRateKey other = (SampleRateKey) obj;
		if (baudRate != other.baudRate)
			return false;
		if (deviceOutput != other.deviceOutput)
			return false;
		return true;
	}

}

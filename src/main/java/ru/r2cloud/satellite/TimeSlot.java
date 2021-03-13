package ru.r2cloud.satellite;

public class TimeSlot {

	private long frequency;
	private long start;
	private long end;

	public long getFrequency() {
		return frequency;
	}
	
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}
	
	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (end ^ (end >>> 32));
		result = prime * result + (int) (start ^ (start >>> 32));
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
		TimeSlot other = (TimeSlot) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

}

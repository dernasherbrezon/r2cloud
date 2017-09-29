package ru.r2cloud.model;

import uk.me.g4dpz.satellite.SatPos;

public class SatPass {

	private SatPos start;
	private SatPos end;

	public SatPos getStart() {
		return start;
	}

	public void setStart(SatPos start) {
		this.start = start;
	}

	public SatPos getEnd() {
		return end;
	}

	public void setEnd(SatPos end) {
		this.end = end;
	}

	@Override
	public String toString() {
		return "[start=" + start.getTime() + ", end=" + end.getTime() + "]";
	}

}

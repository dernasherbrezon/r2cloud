package ru.r2cloud.model;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class SatPass {

	private AbsoluteDate start;
	private AbsoluteDate end;

	public AbsoluteDate getStart() {
		return start;
	}

	public long getStartMillis() {
		return start.toDate(TimeScalesFactory.getUTC()).getTime();
	}

	public void setStart(AbsoluteDate start) {
		this.start = start;
	}
	
	public long getEndMillis() {
		return end.toDate(TimeScalesFactory.getUTC()).getTime();
	}

	public AbsoluteDate getEnd() {
		return end;
	}

	public void setEnd(AbsoluteDate end) {
		this.end = end;
	}

}

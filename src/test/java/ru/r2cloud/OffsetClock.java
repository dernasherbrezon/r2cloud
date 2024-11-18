package ru.r2cloud;

import ru.r2cloud.util.Clock;

public class OffsetClock implements Clock {

	private final long offset;

	public OffsetClock(long initialTime) {
		this.offset = System.currentTimeMillis() - initialTime;
	}

	@Override
	public long millis() {
		return System.currentTimeMillis() - offset;
	}
}

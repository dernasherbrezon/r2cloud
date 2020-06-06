package ru.r2cloud;

import ru.r2cloud.util.Clock;

public class SteppingClock implements Clock {

	private final int step;

	private long current;

	public SteppingClock(long initial, int step) {
		this.step = step;
		this.current = initial;
	}

	@Override
	public long millis() {
		long result = current;
		current += step;
		return result;
	}
}

package ru.r2cloud;

import ru.r2cloud.util.Clock;

public class ManualClock implements Clock {

	private long current;

	public ManualClock() {
		this.current = System.currentTimeMillis();
	}

	public void add(long millis) {
		current += millis;
	}

	@Override
	public long millis() {
		return current;
	}

}

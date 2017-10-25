package ru.r2cloud.util;

public class DefaultClock implements Clock {

	@Override
	public long millis() {
		return System.currentTimeMillis();
	}

}

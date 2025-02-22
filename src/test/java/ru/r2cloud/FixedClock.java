package ru.r2cloud;

import ru.r2cloud.util.Clock;

public class FixedClock implements Clock {

    private long millis;

    public FixedClock(long millis) {
        this.millis = millis;
    }

    @Override
    public long millis() {
        return millis;
    }
    
    public void setMillis(long millis) {
		this.millis = millis;
	}
}

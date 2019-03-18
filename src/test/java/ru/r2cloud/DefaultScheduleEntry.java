package ru.r2cloud;

import ru.r2cloud.satellite.ScheduleEntry;

public class DefaultScheduleEntry implements ScheduleEntry {

	private long startTimeMillis;
	private long endTimeMillis;
	private boolean cancelled = false;
	private String id;

	public DefaultScheduleEntry() {
		// do nothing
	}

	public DefaultScheduleEntry(long startTimeMillis, long endTimeMillis, String id) {
		super();
		this.startTimeMillis = startTimeMillis;
		this.endTimeMillis = endTimeMillis;
		this.id = id;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public void setEndTimeMillis(long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	@Override
	public long getEndTimeMillis() {
		return endTimeMillis;
	}

	@Override
	public void cancel() {
		cancelled = true;
	}

	@Override
	public String getId() {
		return id;
	}

}

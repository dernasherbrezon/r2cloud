package ru.r2cloud.satellite;

public interface ScheduleEntry {

	long getStartTimeMillis();

	long getEndTimeMillis();

	void cancel();

	String getId();

}

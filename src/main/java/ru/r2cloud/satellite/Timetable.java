package ru.r2cloud.satellite;

public interface Timetable {

	boolean addFully(TimeSlot slot);

	TimeSlot addPartially(TimeSlot slot);

	void clear();

	boolean remove(TimeSlot slot);

}

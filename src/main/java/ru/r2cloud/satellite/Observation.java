package ru.r2cloud.satellite;

import java.util.Date;

public interface Observation {

	void start();

	void stop();
	
	void decode();

	Date getStart();
	
	Date getEnd();
	
	String getId();
}

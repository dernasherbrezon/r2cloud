package ru.r2cloud.satellite;

import ru.r2cloud.model.Satellite;

public interface SatelliteFilter {

	boolean accept(Satellite satellite);
	
	String getName();

}

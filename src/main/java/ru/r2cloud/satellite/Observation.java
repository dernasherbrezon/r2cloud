package ru.r2cloud.satellite;

import ru.r2cloud.model.SatPass;

public interface Observation {

	void start();

	void stop();

	SatPass getNextPass();
}

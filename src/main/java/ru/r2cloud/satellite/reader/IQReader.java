package ru.r2cloud.satellite.reader;

import ru.r2cloud.model.IQData;

public interface IQReader {

	IQData start();

	void complete();
	
}

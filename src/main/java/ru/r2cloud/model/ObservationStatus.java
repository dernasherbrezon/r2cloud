package ru.r2cloud.model;

public enum ObservationStatus {

	@Deprecated
	NEW,
	
	RECEIVING_DATA, RECEIVED, DECODED, UPLOADED, FAILED

}

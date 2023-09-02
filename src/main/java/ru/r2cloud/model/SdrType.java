package ru.r2cloud.model;

public enum SdrType {

	RTLSDR, PLUTOSDR, SDRSERVER,

	// be able to load old types from the disk
	@Deprecated
	R2LORA

}

package ru.r2cloud.model;

// use separate device types instead
@Deprecated
public enum SdrType {

	RTLSDR, PLUTOSDR, SDRSERVER,

	// be able to load old types from the disk
	@Deprecated
	R2LORA

}

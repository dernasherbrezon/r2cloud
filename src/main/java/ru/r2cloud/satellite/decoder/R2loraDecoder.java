package ru.r2cloud.satellite.decoder;

import java.io.File;

import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class R2loraDecoder implements Decoder {

	private final Configuration config;

	public R2loraDecoder(Configuration config) {
		this.config = config;
	}

	@Override
	public DecoderResult decode(File rawFile, ObservationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}

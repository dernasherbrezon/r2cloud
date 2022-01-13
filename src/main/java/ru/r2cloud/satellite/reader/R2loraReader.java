package ru.r2cloud.satellite.reader;

import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class R2loraReader implements IQReader {

	private final Configuration config;
	private final ObservationRequest req;

	public R2loraReader(Configuration config, ObservationRequest req) {
		this.config = config;
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void complete() {
		// TODO Auto-generated method stub

	}

}

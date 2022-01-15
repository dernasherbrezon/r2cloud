package ru.r2cloud.device;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.r2lora.R2loraClient;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.satellite.SatelliteFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.R2loraReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactory;

public class LoraDevice extends Device {

	private final R2loraClient client;
	private final Configuration config;

	public LoraDevice(String id, SatelliteFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, RotatorService rotatorService, ObservationDao observationDao, DecoderService decoderService, Configuration config,
			R2loraClient client) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, rotatorService, observationDao, decoderService);
		this.client = client;
		this.config = config;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Satellite satellite) {
		return new R2loraReader(config, req, client, satellite);
	}

}

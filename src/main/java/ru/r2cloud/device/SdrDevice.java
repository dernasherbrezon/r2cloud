package ru.r2cloud.device;

import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.satellite.SatelliteFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.PlutoSdrReader;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.satellite.reader.RtlSdrReader;
import ru.r2cloud.satellite.reader.SdrServerReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class SdrDevice extends Device {

	private final Configuration config;
	private final ProcessFactory processFactory;

	public SdrDevice(String id, SatelliteFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, RotatorService rotatorService, ObservationDao observationDao, DecoderService decoderService, Configuration config,
			ProcessFactory processFactory) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, rotatorService, observationDao, decoderService);
		this.config = config;
		this.processFactory = processFactory;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Satellite satellite) {
		FrequencySource source = req.getSource();
		switch (source) {
		case APT:
			return new RtlFmReader(config, processFactory, req);
		case LRPT:
		case TELEMETRY:
			if (req.getSdrType().equals(SdrType.RTLSDR)) {
				return new RtlSdrReader(config, processFactory, req);
			} else if (req.getSdrType().equals(SdrType.PLUTOSDR)) {
				return new PlutoSdrReader(config, processFactory, req);
			} else if (req.getSdrType().equals(SdrType.SDRSERVER)) {
				return new SdrServerReader(config, req);
			} else {
				throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
			}
		default:
			throw new IllegalArgumentException("unsupported source: " + source);
		}
	}

}

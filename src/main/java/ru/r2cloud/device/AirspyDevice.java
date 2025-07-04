package ru.r2cloud.device;

import java.util.concurrent.locks.ReentrantLock;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.AirspyReader;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.sdr.AirspyStatusProcess;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class AirspyDevice extends Device {

	private final Configuration config;
	private final ProcessFactory processFactory;
	private final ReentrantLock lock;
	private final AirspyStatusProcess airspyStatus;

	public AirspyDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule, Configuration config, ProcessFactory processFactory, ReentrantLock lock, AirspyStatusProcess airspyStatus) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
		this.processFactory = processFactory;
		this.lock = lock;
		this.airspyStatus = airspyStatus;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter satellite, DeviceConfiguration deviceConfiguration) {
		return new AirspyReader(config, deviceConfiguration, processFactory, req, satellite, lock, airspyStatus.getSupportedSampleRates());
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setDeviceName("Airspy");
		if (airspyStatus.getSerialNumber() != null) {
			result.setDeviceName(result.getDeviceName() + " " + airspyStatus.getSerialNumber());
		}
		SdrStatus status = airspyStatus.getStatus();
		result.setFailureMessage(status.getFailureMessage());
		result.setStatus(status.getStatus());
		result.setModel(status.getModel());
		return result;
	}
}

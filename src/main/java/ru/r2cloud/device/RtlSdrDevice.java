package ru.r2cloud.device;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.satellite.reader.RtlSdrReader;
import ru.r2cloud.sdr.RtlStatusProcess;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class RtlSdrDevice extends Device {

	private final Configuration config;
	private final ProcessFactory processFactory;
	private final RtlStatusProcess statusDao;

	public RtlSdrDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule, Configuration config, ProcessFactory processFactory) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
		this.processFactory = processFactory;
		this.statusDao = new RtlStatusProcess(config, processFactory, deviceConfiguration.getRtlDeviceId());
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter transmitter, DeviceConfiguration deviceConfiguration) {
		if (transmitter.getFraming() == Framing.APT) {
			return new RtlFmReader(config, deviceConfiguration, processFactory, req);
		} else {
			return new RtlSdrReader(config, deviceConfiguration, processFactory, req, transmitter);
		}
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		SdrStatus status = statusDao.getStatus();
		result.setFailureMessage(status.getFailureMessage());
		result.setStatus(status.getStatus());
		result.setModel(status.getModel());
		return result;
	}

}

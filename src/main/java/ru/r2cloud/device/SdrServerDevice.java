package ru.r2cloud.device;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
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
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.SdrServerReader;
import ru.r2cloud.sdr.SdrStatusProcess;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactory;

public class SdrServerDevice extends Device {

	private final SdrStatusProcess statusProcess;

	public SdrServerDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.statusProcess = new SdrStatusProcess() {

			@Override
			public SdrStatus getStatus() {
				// sdr-server doesn't support health checks yet
				SdrStatus result = new SdrStatus();
				result.setStatus(DeviceConnectionStatus.CONNECTED);
				return result;
			}
		};
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter transmitter, DeviceConfiguration deviceConfiguration) {
		return new SdrServerReader(req, deviceConfiguration, transmitter);
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		SdrStatus status = statusProcess.getStatus();
		result.setFailureMessage(status.getFailureMessage());
		result.setStatus(status.getStatus());
		result.setModel(status.getModel());
		return result;
	}

}

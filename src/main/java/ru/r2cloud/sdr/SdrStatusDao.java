package ru.r2cloud.sdr;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;

public class SdrStatusDao {

	private SdrStatusProcess statusProcess;

	public SdrStatusDao(Configuration config, ProcessFactory processFactory, int expectedRtlDeviceId) {
		switch (config.getSdrType()) {
		case RTLSDR:
			statusProcess = new RtlStatusProcess(config, processFactory, expectedRtlDeviceId);
			break;
		case PLUTOSDR:
			statusProcess = new PlutoStatusProcess(config, processFactory);
			break;
		case SDRSERVER:
			statusProcess = new SdrStatusProcess() {

				@Override
				public SdrStatus getStatus() {
					// sdr-server doesn't support health checks yet
					SdrStatus result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.CONNECTED);
					return result;
				}
			};
			break;
		default:
			throw new IllegalArgumentException("unsupported sdr type: " + config.getSdrType());
		}
	}

	public SdrStatus getStatus() {
		return statusProcess.getStatus();
	}

}

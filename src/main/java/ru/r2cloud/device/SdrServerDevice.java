package ru.r2cloud.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.BandFrequencyComparator;
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
import ru.r2cloud.util.Util;

public class SdrServerDevice extends Device {

	private static final Logger LOG = LoggerFactory.getLogger(SdrServerDevice.class);
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
	public void reCalculateFrequencyBands(List<Transmitter> scheduledTransmitters) {
		long sdrServerBandwidth = getDeviceConfiguration().getSdrServerConfiguration().getBandwidth();
		long bandwidthCrop = getDeviceConfiguration().getSdrServerConfiguration().getBandwidthCrop();

		List<BandFrequency> bandwidths = new ArrayList<>();
		for (Transmitter curTransmitter : scheduledTransmitters) {
			if (curTransmitter.getBaudRates().isEmpty()) {
				continue;
			}
			Integer maxBaudRate = Collections.max(curTransmitter.getBaudRates());
			if (maxBaudRate == null) {
				continue;
			}
			long sampleRate = Util.getSmallestDividableSampleRate(maxBaudRate, getDeviceConfiguration().getSdrServerConfiguration().getBandwidth());

			BandFrequency cur = new BandFrequency();
			cur.setCenter(curTransmitter.getFrequency());
			cur.setLower(curTransmitter.getFrequency() - sampleRate / 2);
			cur.setUpper(curTransmitter.getFrequency() + sampleRate / 2);
			cur.setTransmitter(curTransmitter);
			bandwidths.add(cur);
		}
		Collections.sort(bandwidths, BandFrequencyComparator.INSTANCE);

		// bands can be calculated only when all supported transmitters known
		BandFrequency currentBand = null;
		List<Transmitter> currentBandTransmitters = null;
		Map<BandFrequency, List<Transmitter>> transmittersPerBand = new HashMap<>();
		for (BandFrequency cur : bandwidths) {
			// first transmitter or upper frequency out of band
			if (currentBand == null || (currentBand.getUpper() - bandwidthCrop) < cur.getUpper()) {
				if (currentBand != null) {
					transmittersPerBand.put(currentBand, currentBandTransmitters);
				}
				currentBandTransmitters = new ArrayList<>();
				currentBand = new BandFrequency();
				currentBand.setLower(cur.getLower() - bandwidthCrop);
				currentBand.setUpper(currentBand.getLower() + sdrServerBandwidth);
				currentBand.setCenter(currentBand.getLower() + (currentBand.getUpper() - currentBand.getLower()) / 2);
			}
			currentBandTransmitters.add(cur.getTransmitter());
		}
		if (currentBand != null) {
			transmittersPerBand.put(currentBand, currentBandTransmitters);
		}
		LOG.info("[{}] active bands are:", id);
		for (Entry<BandFrequency, List<Transmitter>> cur : transmittersPerBand.entrySet()) {
			LOG.info("  {} - {}", cur.getKey().getLower(), cur.getKey().getUpper());
			for (Transmitter curTransmitter : cur.getValue()) {
				curTransmitter.setFrequencyBand(cur.getKey().getCenter());
			}
		}
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

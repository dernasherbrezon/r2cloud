package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);
	public static final int DC_OFFSET = 10_000;

	private final TLEDao tleDao;
	private final PredictOreKit predict;
	private final Configuration config;

	public ObservationFactory(PredictOreKit predict, TLEDao tleDao, Configuration config) {
		this.predict = predict;
		this.tleDao = tleDao;
		this.config = config;
	}

	public List<ObservationRequest> createSchedule(Date date, Satellite satellite) {
		Tle tle = tleDao.findById(satellite.getId());
		if (tle == null) {
			LOG.error("unable to find tle for: {}", satellite);
			return Collections.emptyList();
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(tle.getRaw()[1], tle.getRaw()[2]));
		List<SatPass> batch = predict.calculateSchedule(date, tlePropagator);
		if (batch == null || batch.isEmpty()) {
			return Collections.emptyList();
		}
		List<ObservationRequest> result = new ArrayList<>();
		for (SatPass cur : batch) {
			// ignore all observations out of satellite's active time
			if (satellite.getStart() != null && cur.getEndMillis() < satellite.getStart().getTime()) {
				continue;
			}
			if (satellite.getEnd() != null && cur.getStartMillis() > satellite.getEnd().getTime()) {
				continue;
			}
			result.add(convert(satellite, tle, tlePropagator, cur));
		}
		return result;
	}

	private ObservationRequest convert(Satellite satellite, Tle tle, TLEPropagator tlePropagator, SatPass nextPass) {
		ObservationRequest result = new ObservationRequest();
		result.setSatelliteFrequency(satellite.getFrequency());
		result.setSatelliteId(satellite.getId());
		result.setSource(satellite.getSource());
		result.setTle(tle);
		result.setGroundStation(predict.getPosition().getPoint());
		result.setStartTimeMillis(nextPass.getStartMillis());
		result.setEndTimeMillis(nextPass.getEndMillis());
		result.setId(String.valueOf(result.getStartTimeMillis()) + "-" + satellite.getId());
		// only r2lora can handle lora modulation
		if (satellite.getModulation() != null && satellite.getModulation().equals(Modulation.LORA)) {
			result.setSdrType(SdrType.R2LORA);
		} else {
			result.setSdrType(config.getSdrType());
		}
		if (result.getSdrType().equals(SdrType.R2LORA)) {
			result.setBandwidth(satellite.getLoraBandwidth());
		} else {
			result.setBandwidth(satellite.getBandwidth());
		}
		result.setCenterBandFrequency(satellite.getFrequencyBand().getCenter());
		result.setInputSampleRate(satellite.getInputSampleRate());
		result.setOutputSampleRate(satellite.getOutputSampleRate());

		switch (satellite.getSource()) {
		case APT:
			result.setActualFrequency(result.getSatelliteFrequency());
			break;
		case LRPT:
			result.setActualFrequency(result.getSatelliteFrequency());
			break;
		case TELEMETRY:
			// compensate DC offset only for non sdr-server observations
			if (result.getSdrType().equals(SdrType.SDRSERVER) || result.getSdrType().equals(SdrType.R2LORA)) {
				result.setActualFrequency(result.getSatelliteFrequency());
			} else {
				// at the beginning doppler freq is the max
				long initialDopplerFrequency = predict.getDownlinkFreq(satellite.getFrequency(), nextPass.getStartMillis(), predict.getPosition(), tlePropagator);
				result.setActualFrequency(initialDopplerFrequency + DC_OFFSET);
			}
			break;
		default:
			throw new IllegalArgumentException("unsupported source: " + satellite.getSource());
		}
		return result;
	}

}

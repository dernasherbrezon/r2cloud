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
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);
	public static final int DC_OFFSET = 10_000;
	private static final long MAX_OBSERVATION_MILLIS = 15 * 60 * 1000;

	private final TLEDao tleDao;
	private final PredictOreKit predict;
	private final Configuration config;

	public ObservationFactory(PredictOreKit predict, TLEDao tleDao, Configuration config) {
		this.predict = predict;
		this.tleDao = tleDao;
		this.config = config;
	}

	public List<ObservationRequest> createSchedule(Date date, Transmitter transmitter) {
		Tle tle = tleDao.findById(transmitter.getSatelliteId());
		if (tle == null) {
			LOG.error("unable to find tle for: {}", transmitter.getSatelliteId());
			return Collections.emptyList();
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(tle.getRaw()[1], tle.getRaw()[2]));
		List<SatPass> batch = predict.calculateSchedule(date, tlePropagator);
		if (batch == null || batch.isEmpty()) {
			return Collections.emptyList();
		}
		List<ObservationRequest> result = new ArrayList<>();
		for (SatPass cur : batch) {
			long endMillis = cur.getEndMillis();
			long startMillis = cur.getStartMillis();
			// ignore all observations out of satellite's active time
			if (transmitter.getStart() != null && endMillis < transmitter.getStart().getTime()) {
				continue;
			}
			if (transmitter.getEnd() != null && startMillis > transmitter.getEnd().getTime()) {
				continue;
			}
			// MEO satellites can be visible several hours
			// Raspberry PI might not be able to perform such long observations
			// better split into several
			while (endMillis - startMillis > MAX_OBSERVATION_MILLIS) {
				result.add(convert(transmitter, tle, tlePropagator, startMillis, startMillis + MAX_OBSERVATION_MILLIS));
				startMillis += MAX_OBSERVATION_MILLIS;
			}
			result.add(convert(transmitter, tle, tlePropagator, startMillis, endMillis));
		}
		return result;
	}

	private ObservationRequest convert(Transmitter transmitter, Tle tle, TLEPropagator tlePropagator, long startMillis, long endMillis) {
		ObservationRequest result = new ObservationRequest();
		result.setSatelliteId(transmitter.getSatelliteId());
		result.setTransmitterId(transmitter.getId());
		result.setTle(tle);
		result.setGroundStation(predict.getPosition().getPoint());
		result.setStartTimeMillis(startMillis);
		result.setEndTimeMillis(endMillis);
		result.setId(String.valueOf(result.getStartTimeMillis()) + "-" + transmitter.getId());
		// only r2lora can handle lora modulation
		if (transmitter.getModulation() != null && transmitter.getModulation().equals(Modulation.LORA)) {
			result.setSdrType(SdrType.R2LORA);
		} else {
			result.setSdrType(config.getSdrType());
		}
		result.setCenterBandFrequency(transmitter.getFrequencyBand().getCenter());
		switch (transmitter.getFraming()) {
		case APT:
			result.setActualFrequency(transmitter.getFrequency());
			break;
		case LRPT:
			result.setActualFrequency(transmitter.getFrequency());
			break;
		default:
			// compensate DC offset only for non sdr-server observations
			if (result.getSdrType().equals(SdrType.SDRSERVER) || result.getSdrType().equals(SdrType.R2LORA)) {
				result.setActualFrequency(transmitter.getFrequency());
			} else {
				// at the beginning doppler freq is the max
				long initialDopplerFrequency = predict.getDownlinkFreq(transmitter.getFrequency(), startMillis, predict.getPosition(), tlePropagator);
				result.setActualFrequency(initialDopplerFrequency + DC_OFFSET);
			}
			break;
		}
		return result;
	}

}

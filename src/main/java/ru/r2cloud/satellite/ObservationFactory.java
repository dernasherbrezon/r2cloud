package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);
	private static final long MAX_OBSERVATION_MILLIS = 15 * 60 * 1000;

	private final PredictOreKit predict;

	public ObservationFactory(PredictOreKit predict) {
		this.predict = predict;
	}

	public List<ObservationRequest> createSchedule(AntennaConfiguration antenna, Date date, Transmitter transmitter) {
		if (transmitter.getTle() == null) {
			LOG.error("no tle for: {}", transmitter.getSatelliteId());
			return Collections.emptyList();
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(transmitter.getTle().getRaw()[1], transmitter.getTle().getRaw()[2]));
		List<SatPass> batch = predict.calculateSchedule(antenna, date, tlePropagator);
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
				result.add(convert(transmitter, transmitter.getTle(), startMillis, startMillis + MAX_OBSERVATION_MILLIS));
				startMillis += MAX_OBSERVATION_MILLIS;
			}
			result.add(convert(transmitter, transmitter.getTle(), startMillis, endMillis));
		}
		return result;
	}

	private ObservationRequest convert(Transmitter transmitter, Tle tle, long startMillis, long endMillis) {
		ObservationRequest result = new ObservationRequest();
		result.setSatelliteId(transmitter.getSatelliteId());
		result.setTransmitterId(transmitter.getId());
		result.setTle(tle);
		result.setGroundStation(predict.getPosition().getPoint());
		result.setStartTimeMillis(startMillis);
		result.setEndTimeMillis(endMillis);
		result.setId(String.valueOf(result.getStartTimeMillis()) + "-" + transmitter.getId());
		result.setFrequency(transmitter.getFrequency());
		result.setCenterBandFrequency(transmitter.getFrequencyBand());
		return result;
	}

}

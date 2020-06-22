package ru.r2cloud.satellite;

import java.util.Date;

import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.tle.TLEDao;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);
	public static final int DC_OFFSET = 10_000;

	private final TLEDao tleDao;
	private final PredictOreKit predict;

	public ObservationFactory(PredictOreKit predict, TLEDao tleDao) {
		this.predict = predict;
		this.tleDao = tleDao;
	}

	public ObservationRequest create(Date date, Satellite satellite, boolean immediately) {
		Tle tle = tleDao.findById(satellite.getId());
		if (tle == null) {
			LOG.error("unable to find tle for: {}", satellite.getName());
			return null;
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(tle.getRaw()[1], tle.getRaw()[2]));
		SatPass nextPass = predict.calculateNext(date, tlePropagator);
		if (nextPass == null) {
			LOG.info("can't find next pass for {}", satellite.getName());
			return null;
		}
		ObservationRequest result = new ObservationRequest();
		result.setSatelliteFrequency(satellite.getFrequency());
		result.setSatelliteId(satellite.getId());
		result.setSource(satellite.getSource());
		result.setBandwidth(satellite.getBandwidth());
		result.setTle(tle);
		result.setGroundStation(predict.getPosition().getPoint());
		if (immediately) {
			result.setStartTimeMillis(date.getTime());
			result.setEndTimeMillis(result.getStartTimeMillis() + (nextPass.getEndMillis() - nextPass.getStartMillis()));
		} else {
			result.setStartTimeMillis(nextPass.getStartMillis());
			result.setEndTimeMillis(nextPass.getEndMillis());
		}
		result.setId(String.valueOf(result.getStartTimeMillis()));

		switch (satellite.getSource()) {
		case APT:
			result.setActualFrequency(satellite.getFrequency());
			result.setInputSampleRate(60_000);
			result.setOutputSampleRate(11_025);
			break;
		case LRPT:
			result.setInputSampleRate(288_000);
			result.setOutputSampleRate(144_000);
			result.setActualFrequency(satellite.getFrequency());
			break;
		case FSK_AX25_G3RUH:
		case TELEMETRY:
			result.setInputSampleRate(240_000);
			result.setOutputSampleRate(48_000);
			// at the beginning doppler freq is the max
			long initialDopplerFrequency = predict.getDownlinkFreq(satellite.getFrequency(), nextPass.getStartMillis(), predict.getPosition(), tlePropagator);
			result.setActualFrequency(initialDopplerFrequency + DC_OFFSET);
			break;
		default:
			throw new IllegalArgumentException("unsupported source: " + satellite.getSource());
		}
		return result;
	}

}

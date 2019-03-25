package ru.r2cloud.satellite;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.tle.TLEDao;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);
	public static final int DC_OFFSET = 10_000;
	private static final int BANDWIDTH = 10_000;

	private final TLEDao tleDao;
	private final Predict predict;

	public ObservationFactory(Predict predict, TLEDao tleDao) {
		this.predict = predict;
		this.tleDao = tleDao;
	}

	public ObservationRequest create(Date date, Satellite satellite, boolean immediately) {
		TLE tle = tleDao.findById(satellite.getId());
		if (tle == null) {
			LOG.error("unable to find tle for: {}", satellite.getName());
			return null;
		}
		uk.me.g4dpz.satellite.Satellite libSatellite = SatelliteFactory.createSatellite(tle);
		SatPass nextPass = predict.calculateNext(date, libSatellite);
		if (nextPass == null) {
			LOG.info("can't find next pass for {}", satellite.getName());
			return null;
		}
		ObservationRequest result = new ObservationRequest();
		result.setOrigin(libSatellite);
		result.setSatelliteFrequency(satellite.getFrequency());
		result.setSatelliteId(satellite.getId());
		result.setSource(satellite.getSource());
		result.setStartLatitude(nextPass.getStart().getLatitude());
		result.setEndLatitude(nextPass.getEnd().getLatitude());
		if (immediately) {
			result.setStartTimeMillis(date.getTime());
			result.setEndTimeMillis(result.getStartTimeMillis() + (nextPass.getEnd().getTime().getTime() - nextPass.getStart().getTime().getTime()));
		} else {
			result.setStartTimeMillis(nextPass.getStart().getTime().getTime());
			result.setEndTimeMillis(nextPass.getEnd().getTime().getTime());
		}
		result.setId(String.valueOf(result.getStartTimeMillis()));

		switch (satellite.getSource()) {
		case APT:
			result.setActualFrequency(satellite.getFrequency());
			result.setInputSampleRate(60_000);
			result.setOutputSampleRate(11_025);
			break;
		case LRPT:
			result.setInputSampleRate(240_000);
			result.setOutputSampleRate(150_000);
			result.setActualFrequency(satellite.getFrequency());
			break;
		case TELEMETRY:
			result.setInputSampleRate(240_000);
			result.setOutputSampleRate(96_000);
			// at the beginning doppler freq is the max
			long initialDopplerFrequency = predict.getDownlinkFreq(satellite.getFrequency(), nextPass.getStart().getTime().getTime(), libSatellite);
			result.setActualFrequency(initialDopplerFrequency + BANDWIDTH + DC_OFFSET);
			break;
		default:
			throw new IllegalArgumentException("unsupported source: " + satellite.getSource());
		}
		return result;
	}

}

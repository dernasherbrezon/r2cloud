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

	private final TLEDao tleDao;
	private final Predict predict;

	public ObservationFactory(Predict predict, TLEDao tleDao) {
		this.predict = predict;
		this.tleDao = tleDao;
	}

	public ObservationRequest create(Date date, Satellite satellite) {
		TLE tle = tleDao.findById(satellite.getId());
		if (tle == null) {
			LOG.error("unable to find tle for: " + satellite.getName());
			return null;
		}
		uk.me.g4dpz.satellite.Satellite libSatellite = SatelliteFactory.createSatellite(tle);
		SatPass nextPass = predict.calculateNext(date, libSatellite);
		if (nextPass == null) {
			LOG.info("can't find next pass for " + satellite.getName());
			return null;
		}
		ObservationRequest result = new ObservationRequest();
		result.setOrigin(libSatellite);
		result.setSatelliteFrequency(satellite.getFrequency());
		result.setBandwidth(satellite.getBandwidth());
		result.setSatelliteId(satellite.getId());
		result.setDecoder(satellite.getDecoder());
		result.setStart(nextPass.getStart());
		result.setStartTimeMillis(nextPass.getStart().getTime().getTime());
		result.setId(String.valueOf(result.getStartTimeMillis()));
		result.setEnd(nextPass.getEnd());
		result.setEndTimeMillis(nextPass.getEnd().getTime().getTime());
		String decoder = satellite.getDecoder();
		if (decoder == null) {
			throw new IllegalArgumentException("unknown decoder for: " + satellite.getId());
		}
		if (decoder.equals("apt")) {
			result.setActualFrequency(satellite.getFrequency());
			result.setInputSampleRate(60_000);
			result.setOutputSampleRate(11_025);
		} else if (decoder.equals("lrpt")) {
			result.setInputSampleRate(1_440_000);
			result.setOutputSampleRate(150_000);
			result.setActualFrequency(satellite.getFrequency());
		} else if (decoder.equals("aausat4")) {
			result.setInputSampleRate(240_000);
			result.setOutputSampleRate(32_000);
			// at the beginning doppler freq is the max
			long initialDopplerFrequency = predict.getDownlinkFreq(satellite.getFrequency(), nextPass.getStart().getTime().getTime(), libSatellite);
			result.setActualFrequency(initialDopplerFrequency + satellite.getBandwidth() / 2);
		} else {
			throw new IllegalArgumentException("unsupported decoder: " + decoder);
		}

		return result;
	}

}

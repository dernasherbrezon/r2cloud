package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
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
			result.add(convert(satellite, tle, tlePropagator, cur));
		}
		return result;
	}

	public ObservationRequest create(Date date, Satellite satellite) {
		Tle tle = tleDao.findById(satellite.getId());
		if (tle == null) {
			LOG.error("unable to find tle for: {}", satellite);
			return null;
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(tle.getRaw()[1], tle.getRaw()[2]));
		SatPass nextPass = predict.calculateNext(date, tlePropagator);
		if (nextPass == null) {
			LOG.info("can't find next pass for {}", satellite);
			return null;
		}
		return convert(satellite, tle, tlePropagator, nextPass);
	}

	private ObservationRequest convert(Satellite satellite, Tle tle, TLEPropagator tlePropagator, SatPass nextPass) {
		ObservationRequest result = new ObservationRequest();
		result.setSatelliteFrequency(satellite.getFrequency());
		result.setSatelliteId(satellite.getId());
		result.setSource(satellite.getSource());
		result.setBandwidth(satellite.getBandwidth());
		result.setTle(tle);
		result.setGroundStation(predict.getPosition().getPoint());
		result.setStartTimeMillis(nextPass.getStartMillis());
		result.setEndTimeMillis(nextPass.getEndMillis());
		result.setId(String.valueOf(result.getStartTimeMillis()));
		result.setGain(config.getDouble("satellites.rtlsdr.gain"));
		result.setBiast(config.getBoolean("satellites.rtlsdr.biast"));

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

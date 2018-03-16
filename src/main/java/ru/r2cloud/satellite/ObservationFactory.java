package ru.r2cloud.satellite;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class ObservationFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationFactory.class);

	private final TLEDao tleDao;
	private final Predict predict;
	private final Configuration config;
	private final ProcessFactory factory;
	private final SatelliteDao dao;
	private final APTDecoder aptDecoder;

	public ObservationFactory(Configuration config, Predict predict, TLEDao tleDao, ProcessFactory factory, SatelliteDao dao, APTDecoder aptDecoder) {
		this.config = config;
		this.predict = predict;
		this.tleDao = tleDao;
		this.factory = factory;
		this.dao = dao;
		this.aptDecoder = aptDecoder;
	}

	public Observation create(Date date, Satellite satellite) {
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
		String decoder = satellite.getDecoder();
		if (decoder == null) {
			throw new IllegalArgumentException("unknown decoder for: " + satellite.getId());
		}
		if (decoder.equals("apt")) {
			return new APTObservation(config, satellite, nextPass, factory, dao, aptDecoder);
		} else if (decoder.equals("lrpt")) {
			return new LRPTObservation();
		} else {
			throw new IllegalArgumentException("unsupported decoder: " + decoder);
		}
	}

}

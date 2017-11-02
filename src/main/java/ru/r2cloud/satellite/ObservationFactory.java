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

	public ObservationFactory(Configuration config, Predict predict, TLEDao tleDao, ProcessFactory factory) {
		this.config = config;
		this.predict = predict;
		this.tleDao = tleDao;
		this.factory = factory;
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
		return new Observation(config, satellite, nextPass, factory);
	}

}

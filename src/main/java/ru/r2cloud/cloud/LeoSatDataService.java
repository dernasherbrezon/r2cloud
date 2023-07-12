package ru.r2cloud.cloud;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.util.Configuration;

public class LeoSatDataService {

	private static final Logger LOG = LoggerFactory.getLogger(LeoSatDataService.class);

	private final Configuration config;
	private final IObservationDao dao;
	private final LeoSatDataClient client;
	private final SpectogramService spectogramService;

	public LeoSatDataService(Configuration config, IObservationDao dao, LeoSatDataClient client, SpectogramService spectogramService) {
		this.config = config;
		this.dao = dao;
		this.client = client;
		this.spectogramService = spectogramService;
	}

	public void uploadObservation(Observation observation) {
		String apiKey = config.getProperty("r2cloud.apiKey");
		if (apiKey == null) {
			return;
		}
		LOG.info("[{}] uploading observation", observation.getId());
		Long id = null;
		try {
			id = client.saveMeta(observation);
		} catch (IllegalArgumentException e) {
			// can happen on permanent error on the server side
			// make sure observation won't be uploaded again
			observation.setStatus(ObservationStatus.FAILED);
			dao.update(observation);
			return;
		}
		if (id == null) {
			return;
		}
		if (observation.getDataPath() != null) {
			client.saveBinary(id, observation.getDataPath());
		} else if (observation.getImagePath() != null) {
			client.saveJpeg(id, observation.getImagePath());
		}
		// update status to UPLOADED even if data or spectogram not
		// in future UPLOADED might be split into UPLOADED_META, UPLOADED_DATA,
		// UPLOADED_SPECTOGRAM
		// right now, this seems too complicated
		observation.setStatus(ObservationStatus.UPLOADED);
		dao.update(observation);

		if (config.getBoolean("r2cloud.syncSpectogram")) {
			uploadSpectogram(observation, id);
		}
		LOG.info("[{}] observation uploaded", observation.getId());
	}

	private void uploadSpectogram(Observation observation, Long id) {
		File spectogram;
		if (observation.getSpectogramPath() == null) {
			spectogram = spectogramService.create(observation);
			if (spectogram != null) {
				spectogram = dao.saveSpectogram(observation.getSatelliteId(), observation.getId(), spectogram);
			}
			if (spectogram == null) {
				LOG.info("[{}] unable to save spectogram", observation.getId());
			}
		} else {
			spectogram = observation.getSpectogramPath();
		}
		if (spectogram != null) {
			client.saveSpectogram(id, spectogram);
		}
	}

}

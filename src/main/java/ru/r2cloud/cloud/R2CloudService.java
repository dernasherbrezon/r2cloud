package ru.r2cloud.cloud;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.util.Configuration;

public class R2CloudService {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudService.class);

	private final Configuration config;
	private final ObservationResultDao dao;
	private final R2CloudClient client;
	private final SpectogramService spectogramService;

	public R2CloudService(Configuration config, ObservationResultDao dao, R2CloudClient client, SpectogramService spectogramService) {
		this.config = config;
		this.dao = dao;
		this.client = client;
		this.spectogramService = spectogramService;
	}

	public void uploadObservation(String satelliteId, String observationId) {
		String apiKey = config.getProperty("r2cloud.apiKey");
		if (apiKey == null) {
			return;
		}
		LOG.info("uploading observation: " + observationId);
		ObservationResult observation = dao.find(satelliteId, observationId);
		if (observation == null) {
			LOG.info("observation not found");
			return;
		}
		Long id = client.saveMeta(observation);
		if (id == null) {
			return;
		}
		if (observation.getDataPath() != null) {
			client.saveBinary(id, observation.getDataPath());
		} else if (observation.getaPath() != null) {
			client.saveJpeg(id, observation.getaPath());
		}
		if (config.getBoolean("r2cloud.syncSpectogram")) {
			if (observation.getSpectogramPath() == null) {
				File spectogram = spectogramService.create(observation.getWavPath());
				if (spectogram != null) {
					client.saveSpectogram(id, spectogram);
					if (!dao.saveSpectogram(satelliteId, observationId, spectogram)) {
						LOG.info("unable to save spectogram");
					}
				}
			} else {
				client.saveSpectogram(id, observation.getSpectogramPath());
			}
		}
		LOG.info("observation uploaded: " + observationId);
	}

}

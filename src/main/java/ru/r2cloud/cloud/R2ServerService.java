package ru.r2cloud.cloud;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;

import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.Observation;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.util.Configuration;

public class R2ServerService {

	private static final Logger LOG = LoggerFactory.getLogger(R2ServerService.class);

	private final Configuration config;
	private final ObservationDao dao;
	private final R2ServerClient client;
	private final SpectogramService spectogramService;

	public R2ServerService(Configuration config, ObservationDao dao, R2ServerClient client, SpectogramService spectogramService) {
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
				File spectogram = spectogramService.create(observation);
				if (spectogram != null && !dao.saveSpectogram(observation.getSatelliteId(), observation.getId(), spectogram)) {
					LOG.info("[{}] unable to save spectogram", observation.getId());
				}
			}

			Observation reloadedObservation = dao.find(observation.getSatelliteId(), observation.getId());
			if (reloadedObservation.getSpectogramPath() != null) {
				client.saveSpectogram(id, reloadedObservation.getSpectogramPath());
			}
		}
		LOG.info("[{}] observation uploaded", observation.getId());
	}

	public void saveMetrics(JsonArray metrics) {
		String apiKey = config.getProperty("r2cloud.apiKey");
		if (apiKey == null) {
			return;
		}
		client.saveMetrics(metrics);
	}

}

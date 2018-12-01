package ru.r2cloud.cloud;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;

import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.ObservationFull;
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

	public void uploadObservation(ObservationFull observation) {
		String apiKey = config.getProperty("r2cloud.apiKey");
		if (apiKey == null) {
			return;
		}
		LOG.info("uploading observation: " + observation.getReq().getId());
		Long id = client.saveMeta(observation);
		if (id == null) {
			return;
		}
		if (observation.getResult().getDataPath() != null) {
			client.saveBinary(id, observation.getResult().getDataPath());
		} else if (observation.getResult().getaPath() != null) {
			client.saveJpeg(id, observation.getResult().getaPath());
		}
		if (config.getBoolean("r2cloud.syncSpectogram")) {
			if (observation.getResult().getSpectogramPath() == null) {
				File spectogram = spectogramService.create(observation.getResult().getWavPath());
				if (spectogram != null) {
					client.saveSpectogram(id, spectogram);
					if (!dao.saveSpectogram(observation.getReq().getSatelliteId(), observation.getReq().getId(), spectogram)) {
						LOG.info("unable to save spectogram");
					}
				}
			} else {
				client.saveSpectogram(id, observation.getResult().getSpectogramPath());
			}
		}
		LOG.info("observation uploaded: " + observation.getReq().getId());
	}

	public void saveMetrics(JsonArray metrics) {
		String apiKey = config.getProperty("r2cloud.apiKey");
		if (apiKey == null) {
			return;
		}
		client.saveMetrics(metrics);
	}

}

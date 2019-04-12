package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.ObservationResultDao;

public class DecoderTask {
	
	private static final Logger LOG = LoggerFactory.getLogger(DecoderTask.class);

	private final Map<String, Decoder> decoders;
	private final ObservationResultDao dao;
	private final R2ServerService r2cloudService;

	public DecoderTask(Map<String, Decoder> decoders, ObservationResultDao dao, R2ServerService r2cloudService) {
		this.decoders = decoders;
		this.dao = dao;
		this.r2cloudService = r2cloudService;
	}

	public void run(File dataFile, ObservationRequest observation) {
		Decoder decoder = decoders.get(observation.getSatelliteId());
		if (decoder == null) {
			LOG.error("[{}] unknown decoder for {}", observation.getId(), observation.getSatelliteId());
			return;
		}
		LOG.info("[{}] decoding", observation.getId());
		ObservationResult result = decoder.decode(dataFile, observation);
		LOG.info("[{}] decoded", observation.getId());
		
		if (result.getDataPath() != null) {
			result.setDataPath(dao.saveData(observation.getSatelliteId(), observation.getId(), result.getDataPath()));
		}
		if (result.getaPath() != null) {
			result.setaPath(dao.saveImage(observation.getSatelliteId(), observation.getId(), result.getaPath()));
		}
		
		ObservationFull full = dao.find(observation.getSatelliteId(), observation.getId());
		full.setResult(result);
		dao.update(full);
		r2cloudService.uploadObservation(full);		
	}

}

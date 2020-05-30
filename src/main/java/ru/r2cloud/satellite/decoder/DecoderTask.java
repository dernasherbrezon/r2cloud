package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.satellite.ObservationDao;

public class DecoderTask {
	
	private static final Logger LOG = LoggerFactory.getLogger(DecoderTask.class);

	private final Map<String, Decoder> decoders;
	private final ObservationDao dao;
	private final R2ServerService r2cloudService;

	public DecoderTask(Map<String, Decoder> decoders, ObservationDao dao, R2ServerService r2cloudService) {
		this.decoders = decoders;
		this.dao = dao;
		this.r2cloudService = r2cloudService;
	}

	public void run(File dataFile, ObservationRequest request) {
		Decoder decoder = decoders.get(request.getSatelliteId());
		if (decoder == null) {
			LOG.error("[{}] unknown decoder for {}", request.getId(), request.getSatelliteId());
			return;
		}
		LOG.info("[{}] decoding", request.getId());
		DecoderResult result = decoder.decode(dataFile, request);
		LOG.info("[{}] decoded", request.getId());
		
		if (result.getDataPath() != null) {
			result.setDataPath(dao.saveData(request.getSatelliteId(), request.getId(), result.getDataPath()));
		}
		if (result.getaPath() != null) {
			result.setaPath(dao.saveImage(request.getSatelliteId(), request.getId(), result.getaPath()));
		}
		
		Observation observation = dao.find(request.getSatelliteId(), request.getId());
		observation.setWavPath(result.getWavPath());
		observation.setIqPath(result.getIqPath());
		observation.setGain(result.getGain());
		observation.setChannelA(result.getChannelA());
		observation.setChannelB(result.getChannelB());
		observation.setNumberOfDecodedPackets(result.getNumberOfDecodedPackets());
		observation.setaPath(result.getaPath());
		observation.setDataPath(result.getDataPath());
		observation.setStatus(ObservationStatus.DECODED);
		
		dao.update(observation);
		r2cloudService.uploadObservation(observation);		
	}

}

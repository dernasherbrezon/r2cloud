package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class DecoderService implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(DecoderService.class);

	private ScheduledExecutorService decoderThread = null;

	private final Map<String, Decoder> decoders;
	private final ObservationDao dao;
	private final R2ServerService r2cloudService;
	private final ThreadPoolFactory threadpoolFactory;
	private final Configuration config;

	public DecoderService(Configuration config, Map<String, Decoder> decoders, ObservationDao dao, R2ServerService r2cloudService, ThreadPoolFactory threadpoolFactory) {
		this.config = config;
		this.decoders = decoders;
		this.dao = dao;
		this.r2cloudService = r2cloudService;
		this.threadpoolFactory = threadpoolFactory;
	}

	@Override
	public synchronized void start() {
		decoderThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
		List<Observation> all = dao.findAll();
		String apiKey = config.getProperty("r2cloud.apiKey");
		for (Observation cur : all) {
			if (cur.getStatus().equals(ObservationStatus.NEW)) {
				LOG.info("resuming decoding: {}", cur.getId());
				run(cur.getRawPath(), cur.getReq());
			}
			if (apiKey != null && cur.getStatus().equals(ObservationStatus.DECODED)) {
				LOG.info("resume uploading: {}", cur.getId());
				decoderThread.execute(new SafeRunnable() {

					@Override
					public void safeRun() {
						r2cloudService.uploadObservation(cur);
					}
				});
			}
		}
	}

	public void run(File dataFile, ObservationRequest request) {
		decoderThread.execute(new SafeRunnable() {

			@Override
			public void safeRun() {
				runInternally(dataFile, request);
			}
		});
	}

	private void runInternally(File rawFile, ObservationRequest request) {
		Decoder decoder = decoders.get(request.getSatelliteId());
		if (decoder == null) {
			LOG.error("[{}] unknown decoder for {}", request.getId(), request.getSatelliteId());
			return;
		}
		LOG.info("[{}] decoding", request.getId());
		DecoderResult result = decoder.decode(rawFile, request);
		LOG.info("[{}] decoded", request.getId());

		if (result.getDataPath() != null) {
			result.setDataPath(dao.saveData(request.getSatelliteId(), request.getId(), result.getDataPath()));
		}
		if (result.getImagePath() != null) {
			result.setImagePath(dao.saveImage(request.getSatelliteId(), request.getId(), result.getImagePath()));
		}

		Observation observation = dao.find(request.getSatelliteId(), request.getId());
		observation.setRawPath(result.getRawPath());
		observation.setGain(result.getGain());
		observation.setChannelA(result.getChannelA());
		observation.setChannelB(result.getChannelB());
		observation.setNumberOfDecodedPackets(result.getNumberOfDecodedPackets());
		observation.setImagePath(result.getImagePath());
		observation.setDataPath(result.getDataPath());
		observation.setStatus(ObservationStatus.DECODED);

		dao.update(observation);
		r2cloudService.uploadObservation(observation);
	}

	@Override
	public synchronized void stop() {
		Util.shutdown(decoderThread, config.getThreadPoolShutdownMillis());
	}
}

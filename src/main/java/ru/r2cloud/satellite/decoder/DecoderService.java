package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class DecoderService implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(DecoderService.class);

	private ScheduledExecutorService decoderThread = null;

	private final Decoders decoders;
	private final IObservationDao dao;
	private final LeoSatDataService r2cloudService;
	private final ThreadPoolFactory threadpoolFactory;
	private final Configuration config;
	private final Metrics metrics;
	private final SatelliteDao satelliteDao;

	private Counter lrpt;
	private Counter telemetry;

	public DecoderService(Configuration config, Decoders decoders, IObservationDao dao, LeoSatDataService r2cloudService, ThreadPoolFactory threadpoolFactory, Metrics metrics, SatelliteDao satelliteDao) {
		this.config = config;
		this.decoders = decoders;
		this.dao = dao;
		this.r2cloudService = r2cloudService;
		this.threadpoolFactory = threadpoolFactory;
		this.metrics = metrics;
		this.satelliteDao = satelliteDao;
	}

	@Override
	public synchronized void start() {
		lrpt = metrics.getRegistry().counter("lrpt");
		telemetry = metrics.getRegistry().counter("telemetry");
		decoderThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
	}

	public void retryObservations() {
		if (decoderThread == null) {
			return;
		}
		LOG.info("check for not processed observations");
		List<Observation> all = dao.findAll();
		String apiKey = config.getProperty("r2cloud.apiKey");
		for (Observation cur : all) {
			if (cur.getStatus().equals(ObservationStatus.RECEIVED)) {
				LOG.info("resuming decoding: {}", cur.getId());
				if (cur.getRawPath() == null) {
					LOG.info("raw file doesn't exist: {}", cur.getId());
					cur.setStatus(ObservationStatus.FAILED);
					dao.update(cur);
				} else {
					decode(cur.getSatelliteId(), cur.getId());
				}
			}
			if (apiKey != null && cur.getStatus().equals(ObservationStatus.DECODED)) {
				LOG.info("resume uploading: {}", cur.getId());
				resumeUploading(cur.getSatelliteId(), cur.getId());
			}
		}
	}

	// Skip double decode/upload using single threaded decoderThread as a queue
	// Check status first because previous attempts might update the state on disk
	private void resumeUploading(String satelliteId, String observationId) {
		decoderThread.execute(new SafeRunnable() {

			@Override
			public void safeRun() {
				Observation observation = dao.find(satelliteId, observationId);
				if (observation == null) {
					return;
				}
				if (observation.getStatus().equals(ObservationStatus.DECODED)) {
					r2cloudService.uploadObservation(observation);
				}
			}
		});
	}

	public void decode(String satelliteId, String observationId) {
		decoderThread.execute(new SafeRunnable() {

			@Override
			public void safeRun() {
				Observation observation = dao.find(satelliteId, observationId);
				if (observation == null) {
					return;
				}
				if (observation.getStatus().equals(ObservationStatus.RECEIVED)) {
					decodeInternally(observation.getRawPath(), observation);
				}
			}
		});
	}

	private void decodeInternally(File rawFile, Observation request) {
		Satellite satellite = satelliteDao.findById(request.getSatelliteId());
		if (satellite == null) {
			LOG.error("[{}] satellite is missing. cannot decode: {}", request.getId(), request.getSatelliteId());
			return;
		}
		Transmitter transmitter = null;
		if (request.getTransmitterId() != null) {
			transmitter = satellite.getById(request.getTransmitterId());
		} else {
			// support for legacy observations
			// select first transmitter
			if (satellite.getTransmitters().size() > 0) {
				transmitter = satellite.getTransmitters().get(0);
			}
		}
		if (transmitter == null) {
			LOG.error("[{}] cannot find transmitter for satellite {}", request.getId(), request.getSatelliteId());
			return;
		}
		Decoder decoder = decoders.findByTransmitter(transmitter);
		if (decoder == null) {
			LOG.error("[{}] unknown decoder for {} transmitter {}", request.getId(), request.getSatelliteId(), request.getTransmitterId());
			return;
		}
		if (!rawFile.getParentFile().exists()) {
			LOG.info("[{}] observation no longer exist. This can be caused by slow decoding of other observations and too aggressive retention. Increase scheduler.data.retention.count or reduce number of scheduled satellites or use faster hardware", request.getId());
			return;
		}
		if (!rawFile.exists()) {
			LOG.info("[{}] raw data for observation is missing. This can be caused by slow decoding of other observations and too aggressive retention. Increase scheduler.data.retention.raw.count or reduce number of scheduled satellites or use faster hardware", request.getId());
			return;
		}
		LOG.info("[{}] decoding", request.getId());
		DecoderResult result = decoder.decode(rawFile, request, transmitter);
		LOG.info("[{}] decoded", request.getId());

		if (result.getDataPath() != null) {
			result.setDataPath(dao.saveData(request.getSatelliteId(), request.getId(), result.getDataPath()));
		}
		if (result.getImagePath() != null) {
			result.setImagePath(dao.saveImage(request.getSatelliteId(), request.getId(), result.getImagePath()));
		}

		Observation observation = dao.find(request.getSatelliteId(), request.getId());
		if (observation == null) {
			LOG.info("[{}] observation was deleted before any data saved", request.getId());
			return;
		}
		observation.setRawPath(result.getRawPath());
		observation.setChannelA(result.getChannelA());
		observation.setChannelB(result.getChannelB());
		observation.setNumberOfDecodedPackets(result.getNumberOfDecodedPackets());
		observation.setImagePath(result.getImagePath());
		observation.setDataPath(result.getDataPath());
		observation.setStatus(ObservationStatus.DECODED);

		dao.update(observation);
		r2cloudService.uploadObservation(observation);

		switch (transmitter.getFraming()) {
		case APT:
			break;
		case LRPT:
			lrpt.inc(observation.getNumberOfDecodedPackets());
			break;
		default:
			telemetry.inc(observation.getNumberOfDecodedPackets());
			break;
		}
	}

	@Override
	public synchronized void stop() {
		Util.shutdown(decoderThread, config.getThreadPoolShutdownMillis());
	}
}

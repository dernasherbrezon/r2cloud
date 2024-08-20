package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.cloud.InfluxDBClient;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.Page;
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
	private final SatelliteDao satelliteDao;
	private final InfluxDBClient influxClient;

	public DecoderService(Configuration config, Decoders decoders, IObservationDao dao, LeoSatDataService r2cloudService, ThreadPoolFactory threadpoolFactory, InfluxDBClient influxClient, SatelliteDao satelliteDao) {
		this.config = config;
		this.decoders = decoders;
		this.dao = dao;
		this.r2cloudService = r2cloudService;
		this.threadpoolFactory = threadpoolFactory;
		this.influxClient = influxClient;
		this.satelliteDao = satelliteDao;
	}

	@Override
	public synchronized void start() {
		decoderThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
	}

	public void retryObservations() {
		if (decoderThread == null) {
			return;
		}
		LOG.info("check for not processed observations");
		String apiKey = config.getProperty("r2cloud.apiKey");
		for (Observation cur : dao.findAll(new Page())) {
			if (cur.getStatus().equals(ObservationStatus.RECEIVED)) {
				LOG.info("resuming decoding: {}", cur.getId());
				decode(cur.getSatelliteId(), cur.getId());
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
					boolean decoded = decodeInternally(observation.getRawPath(), observation);
					if (!decoded) {
						observation.setStatus(ObservationStatus.FAILED);
						dao.update(observation);
					}
				}
			}
		});
	}

	private boolean decodeInternally(File rawFile, Observation observation) {
		Satellite satellite = satelliteDao.findById(observation.getSatelliteId());
		if (satellite == null) {
			LOG.error("[{}] satellite is missing. cannot decode: {}", observation.getId(), observation.getSatelliteId());
			return false;
		}
		Transmitter transmitter = satellite.getById(observation.getTransmitterId());
		if (transmitter == null) {
			LOG.error("[{}] cannot find transmitter for satellite {}", observation.getId(), observation.getSatelliteId());
			return false;
		}
		Decoder decoder = decoders.findByTransmitter(transmitter);
		if (decoder == null) {
			LOG.error("[{}] unknown decoder for {} transmitter {}", observation.getId(), observation.getSatelliteId(), observation.getTransmitterId());
			return false;
		}
		if (rawFile == null || !rawFile.getParentFile().exists()) {
			LOG.info("[{}] observation no longer exist. This can be caused by slow decoding of other observations and too aggressive retention. Increase scheduler.data.retention.count or reduce number of scheduled satellites or use faster hardware", observation.getId());
			return false;
		}
		if (!rawFile.exists()) {
			LOG.info("[{}] raw data for observation is missing. This can be caused by slow decoding of other observations and too aggressive retention. Increase scheduler.data.retention.raw.count or reduce number of scheduled satellites or use faster hardware", observation.getId());
			return false;
		}
		LOG.info("[{}] decoding", observation.getId());
		DecoderResult result = decoder.decode(rawFile, observation, transmitter);
		LOG.info("[{}] decoded packets {}", observation.getId(), result.getNumberOfDecodedPackets());

		if (result.getDataPath() != null) {
			result.setDataPath(dao.saveData(observation.getSatelliteId(), observation.getId(), result.getDataPath()));
		}
		if (result.getImagePath() != null) {
			result.setImagePath(dao.saveImage(observation.getSatelliteId(), observation.getId(), result.getImagePath()));
		}

		observation.setRawPath(result.getRawPath());
		if (observation.getRawPath() == null) {
			observation.setRawURL(null);
		}
		observation.setChannelA(result.getChannelA());
		observation.setChannelB(result.getChannelB());
		observation.setNumberOfDecodedPackets(result.getNumberOfDecodedPackets());
		observation.setImagePath(result.getImagePath());
		observation.setDataPath(result.getDataPath());
		observation.setStatus(ObservationStatus.DECODED);

		dao.update(observation);
		r2cloudService.uploadObservation(observation);

		if (observation.getNumberOfDecodedPackets() != null) {
			switch (transmitter.getFraming()) {
			case APT:
				break;
			default:
				influxClient.send(observation, satellite);
				break;
			}
		}

		return true;
	}

	@Override
	public synchronized void stop() {
		Util.shutdown(decoderThread, config.getThreadPoolShutdownMillis());
	}
}

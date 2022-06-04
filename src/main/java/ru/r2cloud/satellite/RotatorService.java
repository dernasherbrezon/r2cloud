package ru.r2cloud.satellite;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.RotatorStatus;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.rotctrld.RotctrldClient;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class RotatorService implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(RotatorService.class);

	private ScheduledExecutorService executor = null;
	private RotctrldClient rotClient;

	private final RotatorStatus status = new RotatorStatus();
	private final RotatorConfiguration config;
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadpoolFactory;
	private final Clock clock;

	public RotatorService(RotatorConfiguration config, PredictOreKit predict, ThreadPoolFactory threadpoolFactory, Clock clock) {
		this.predict = predict;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.config = config;
	}

	@Override
	public synchronized void start() {
		LOG.info("[{}] starting rotator on: {}:{}", config.getId(), config.getHostname(), config.getPort());
		status.setHostport(config.getHostname() + ":" + config.getPort());
		ensureClientConnected();
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rotator"));
	}

	@Override
	public synchronized void stop() {
		if (executor != null) {
			Util.shutdown(executor, threadpoolFactory.getThreadPoolShutdownMillis());
		}
		if (rotClient != null) {
			rotClient.stop();
		}
		LOG.info("[{}] stopped", config.getId());
	}

	public Future<?> schedule(ObservationRequest req, long current) {
		if (executor == null) {
			return null;
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(req.getTle().getRaw()[1], req.getTle().getRaw()[2]));
		TopocentricFrame groundStation = predict.getPosition(req.getGroundStation());
		Future<?> result = executor.scheduleAtFixedRate(new Runnable() {

			private Position previousPosition;

			@Override
			public void run() {
				long current = clock.millis();
				if (current > req.getEndTimeMillis()) {
					LOG.info("[{}] observation time passed. cancelling rotation", req.getId());
					throw new RuntimeException("observation time passed");
				}
				Position currentPosition = predict.getSatellitePosition(current, groundStation, tlePropagator);
				if (previousPosition != null) {
					double tolerance = config.getTolerance();
					double azimuthDelta = Math.abs(currentPosition.getAzimuth() - previousPosition.getAzimuth());
					double elevationDelta = Math.abs(currentPosition.getElevation() - previousPosition.getElevation());
					if (azimuthDelta < tolerance && elevationDelta < tolerance) {
						return;
					}
				} else {
					LOG.info("[{}] moving rotator to {}", req.getId(), currentPosition);
				}

				ensureClientConnected();
				if (RotatorService.this.rotClient != null) {
					try {
						RotatorService.this.rotClient.setPosition(currentPosition);
						previousPosition = currentPosition;
					} catch (Exception e) {
						LOG.error("unable to set rotator position: {}", currentPosition, e);
						RotatorService.this.rotClient.stop();
						RotatorService.this.rotClient = null;
					}
				}
			}
		}, req.getStartTimeMillis() - current, config.getCycleMillis(), TimeUnit.MILLISECONDS);
		return result;
	}

	public RotatorStatus getStatus() {
		return status;
	}

	private synchronized void ensureClientConnected() {
		if (this.rotClient != null) {
			return;
		}
		RotctrldClient result = new RotctrldClient(config.getHostname(), config.getPort(), config.getTimeout());
		try {
			result.start();
			status.setStatus(DeviceConnectionStatus.CONNECTED);
			status.setModel(result.getModelName());
			LOG.info("[{}] initialized for model: {}", config.getId(), status.getModel());
			this.rotClient = result;
			return;
		} catch (Exception e) {
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage(e.getMessage());
			Util.logIOException(LOG, "unable to connect to rotctrld", e);
			return;
		}
	}

}
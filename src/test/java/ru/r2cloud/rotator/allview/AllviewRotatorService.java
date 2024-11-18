package ru.r2cloud.rotator.allview;

import java.io.IOException;
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
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class AllviewRotatorService implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(AllviewRotatorService.class);

	private ScheduledExecutorService executor = null;

	private final RotatorStatus status = new RotatorStatus();
	private final RotatorConfiguration config;
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadpoolFactory;
	private final Clock clock;
	private final AllviewClient client;

	public AllviewRotatorService(AllviewClient client, RotatorConfiguration config, PredictOreKit predict, ThreadPoolFactory threadpoolFactory, Clock clock) {
		this.predict = predict;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.config = config;
		this.client = client;
	}

	@Override
	public synchronized void start() {
		LOG.info("[{}] starting rotator on: {}:{}", config.getId(), config.getHostname(), config.getPort());
		status.setHostport(config.getHostname() + ":" + config.getPort());
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rotator"));
	}

	@Override
	public synchronized void stop() {
		if (executor != null) {
			Util.shutdown(executor, threadpoolFactory.getThreadPoolShutdownMillis());
		}
		client.stop();
		LOG.info("[{}] stopped", config.getId());
	}

	public Future<?> schedule(ObservationRequest req, long current, Future<?> startFuture) {
		if (executor == null) {
			return null;
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(req.getTle().getRaw()[1], req.getTle().getRaw()[2]));
		TopocentricFrame groundStation = predict.getPosition(req.getGroundStation());
		Future<?> result = executor.scheduleAtFixedRate(new Runnable() {

			private Position previousPosition;
			private boolean log = true;

			@Override
			public void run() {
				long current = clock.millis();
				if (current > req.getEndTimeMillis()) {
					LOG.info("[{}] observation time passed. cancelling rotation", req.getId());
					client.stop();
					throw new RuntimeException("observation time passed");
				}
				if (startFuture != null && startFuture.isDone()) {
					LOG.info("[{}] observation stopped. cancelling rotation", req.getId());
					client.stop();
					throw new RuntimeException("observation stopped");
				}

				if (!ensureClientConnected(req.getId(), log)) {
					log = false;
					// try reconnect on the next iteration
					return;
				}

				Position nextPosition;
				if (previousPosition == null) {

					try {
						client.stopMotors();

						previousPosition = predict.getSatellitePosition(current, groundStation, tlePropagator);

						client.waitMotorStop();

						client.setPosition(previousPosition);

						client.waitMotorStop();

					} catch (IOException e) {
						LOG.error("move to initial position", e);
						client.stop();
						// force reconnect on the next iteration
						status.setStatus(DeviceConnectionStatus.FAILED);
						status.setFailureMessage(e.getMessage());
						previousPosition = null;
						return;
					}
				}

				nextPosition = predict.getSatellitePosition(current + config.getCycleMillis(), groundStation, tlePropagator);
				if (nextPosition.getElevation() < 0.0) {
					LOG.info("[{}] negative elevation requested. most likely stale or invalid tle. cancelling rotation", req.getId());
					throw new RuntimeException("negative elevation");
				}

				try {
					client.slew(nextPosition, previousPosition);
					previousPosition = nextPosition;
				} catch (IOException e) {
					LOG.error("can't slew", e);
					client.stop();
					// force reconnect on the next iteration
					status.setStatus(DeviceConnectionStatus.FAILED);
					status.setFailureMessage(e.getMessage());
					previousPosition = null;
					return;
				}

			}

		}, req.getStartTimeMillis() - current, config.getCycleMillis(), TimeUnit.MILLISECONDS);
		return result;
	}

	private synchronized boolean ensureClientConnected(String id, boolean shouldLogError) {
		if (status.getStatus().equals(DeviceConnectionStatus.CONNECTED)) {
			return true;
		}
		try {
			client.start();
			status.setStatus(DeviceConnectionStatus.CONNECTED);
			long start = clock.millis();
			String model = client.getMotorBoardVersion();
			long latency = clock.millis() - start;
			status.setModel(model);
			LOG.info("[{}] initialized for model: {}. communication latency: {} ms", config.getId(), status.getModel(), latency);
			return true;
		} catch (Exception e) {
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage(e.getMessage());
			if (shouldLogError) {
				if (id != null) {
					Util.logIOException(LOG, "[" + id + "] unable to connect to rotctrld", e);
				} else {
					Util.logIOException(LOG, "unable to connect to rotctrld", e);
				}
			}
			return false;
		}
	}

	public RotatorStatus getStatus() {
		return status;
	}

}
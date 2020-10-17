package ru.r2cloud.satellite;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.rotctrld.RotctrldClient;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class RotatorService implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(RotatorService.class);

	private ScheduledExecutorService executor = null;
	private boolean enabled;
	private String rotatorHostname;
	private int rotatorPort;
	private RotctrldClient rotClient;
	private String failureMessage;

	private final Configuration config;
	private final PredictOreKit predict;
	private final ThreadPoolFactory threadpoolFactory;
	private final Clock clock;

	public RotatorService(Configuration config, PredictOreKit predict, ThreadPoolFactory threadpoolFactory, Clock clock, Metrics metrics) {
		this.config = config;
		this.predict = predict;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.rotatorHostname = config.getProperty("rotator.rotctrld.hostname");
		this.rotatorPort = config.getInteger("rotator.rotctrld.port");
		this.rotClient = new RotctrldClient(rotatorHostname, rotatorPort, config.getInteger("rotator.rotctrld.timeout"));
		this.enabled = config.getBoolean("rotator.enabled");
		this.config.subscribe(this, "rotator.enabled", "rotator.rotctrld.hostname", "rotator.rotctrld.port");
		metrics.getHealthRegistry().register("rotctrld", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				if (failureMessage == null) {
					if (enabled) {
						return ResultUtil.healthy();
					} else {
						return ResultUtil.unknown();
					}
				} else {
					return ResultUtil.unhealthy(failureMessage);
				}
			}
		});
	}

	@Override
	public void onConfigUpdated() {
		stop();
		enabled = config.getBoolean("rotator.enabled");
		rotatorHostname = config.getProperty("rotator.rotctrld.hostname");
		rotatorPort = config.getInteger("rotator.rotctrld.port");
		start();
	}

	@Override
	public synchronized void start() {
		if (!enabled) {
			LOG.info("rotator is disabled");
			return;
		}
		LOG.info("rotator is enabled on: {}:{}", rotatorHostname, rotatorPort);
		try {
			rotClient = new RotctrldClient(rotatorHostname, rotatorPort, config.getInteger("rotator.rotctrld.timeout"));
			rotClient.start();
			String modelName = rotClient.getModelName();
			LOG.info("initialized for model: {}", modelName);
		} catch (Exception e) {
			failureMessage = "unable to connect to rotctrld";
			Util.logIOException(LOG, failureMessage, e);
			enabled = false;
			return;
		}
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rotator"));
	}

	@Override
	public synchronized void stop() {
		if (!enabled) {
			return;
		}
		if (executor != null) {
			Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		}
		if (rotClient != null) {
			rotClient.stop();
		}
		LOG.info("stopped");
	}

	public Future<?> schedule(ObservationRequest req, long current) {
		if (!enabled) {
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
					double tolerance = config.getDouble("rotator.tolerance");
					double azimuthDelta = Math.abs(currentPosition.getAzimuth() - previousPosition.getAzimuth());
					double elevationDelta = Math.abs(currentPosition.getElevation() - previousPosition.getElevation());
					if (azimuthDelta < tolerance && elevationDelta < tolerance) {
						return;
					}
				} else {
					LOG.info("[{}] moving rotator to {}", req.getId(), currentPosition);
				}

				try {
					rotClient.setPosition(currentPosition);
				} catch (Exception e) {
					LOG.error("unable to set rotator position: {}. cancelling rotation", currentPosition, e);
					throw new RuntimeException(e);
				}

				previousPosition = currentPosition;
			}
		}, req.getStartTimeMillis() - current, config.getInteger("rotator.cycleMillis"), TimeUnit.MILLISECONDS);
		return result;
	}

}

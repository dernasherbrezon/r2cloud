package ru.r2cloud.device;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class DeviceManager implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);

	private final Configuration config;
	private final SatelliteDao dao;
	private final ThreadPoolFactory threadpoolFactory;
	private final List<Device> devices = new ArrayList<>();
	private ScheduledExecutorService rescheduleThread = null;

	private int currentDevice = 0;

	public DeviceManager(Configuration config, SatelliteDao dao, ThreadPoolFactory threadpoolFactory) {
		this.dao = dao;
		this.config = config;
		this.threadpoolFactory = threadpoolFactory;
		this.config.subscribe(this, "locaiton.lat");
		this.config.subscribe(this, "locaiton.lon");
	}

	public void addDevice(Device device) {
		this.devices.add(device);
	}

	@Override
	public void start() {
		List<Satellite> all = dao.findEnabled();
		for (Satellite cur : all) {
			for (int i = 0; i < devices.size(); i++) {
				if (next().trySatellite(cur)) {
					break;
				}
			}
		}
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).start();
		}
		rescheduleThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("re-schedule"));
		LOG.info("observations rescheduled. next update at: {}", new Date(System.currentTimeMillis() + (long) PredictOreKit.PREDICT_INTERVAL));
		synchronized (this) {
			rescheduleThread.schedule(new SafeRunnable() {

				@Override
				public void safeRun() {
					for (int i = 0; i < devices.size(); i++) {
						devices.get(i).reschedule();
					}
				}
			}, (long) PredictOreKit.PREDICT_INTERVAL, TimeUnit.MILLISECONDS);
		}
	}

	public ObservationRequest schedule(Satellite satellite) {
		for (int i = 0; i < devices.size(); i++) {
			ObservationRequest req = next().enableSatellite(satellite);
			if (req != null) {
				return req;
			}
		}
		return null;
	}

	public void disableSatellite(Satellite satelliteToEdit) {
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).disableSatellite(satelliteToEdit);
		}
	}

	private Device next() {
		if (currentDevice >= devices.size()) {
			currentDevice = 0;
		}
		Device result = devices.get(currentDevice);
		currentDevice++;
		return result;
	}

	public ObservationRequest startImmediately(Satellite satellite) {
		for (int i = 0; i < devices.size(); i++) {
			ObservationRequest result = devices.get(i).startImmediately(satellite);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public boolean completeImmediately(String observationId) {
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).completeImmediately(observationId)) {
				return true;
			}
		}
		return false;
	}

	public ObservationRequest findFirstBySatelliteId(String satelliteId, long current) {
		for (int i = 0; i < devices.size(); i++) {
			ObservationRequest result = devices.get(i).findFirstBySatelliteId(satelliteId, current);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public void stop() {
		Util.shutdown(rescheduleThread, config.getThreadPoolShutdownMillis());
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).stop();
		}
	}

	@Override
	public void onConfigUpdated() {
		if (config.getProperty("locaiton.lat") != null && config.getProperty("locaiton.lon") != null) {
			LOG.info("base station location changed. reschedule");
			stop();
			start();
		} else {
			LOG.info("missing location. cancelling all observations");
			stop();
		}
	}

}

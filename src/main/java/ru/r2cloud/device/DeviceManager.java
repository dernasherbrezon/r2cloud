package ru.r2cloud.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.DeviceStatusComparator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Clock;
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
	private final Clock clock;
	private ScheduledExecutorService rescheduleThread = null;

	private int currentDevice = 0;

	public DeviceManager(Configuration config, SatelliteDao dao, ThreadPoolFactory threadpoolFactory, Clock clock) {
		this.dao = dao;
		this.config = config;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.config.subscribe(this, "locaiton.lat");
		this.config.subscribe(this, "locaiton.lon");
	}

	public void addDevice(Device device) {
		this.devices.add(device);
	}

	@Override
	public void start() {
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).start();
		}
		rescheduleThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("re-schedule"));
		reschedule();
	}

	private void reschedule() {
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).removeAllTransmitters();
		}
		List<Satellite> all = dao.findEnabled();
		for (Satellite cur : all) {
			for (Transmitter curTransmitter : cur.getTransmitters()) {
				for (int i = 0; i < devices.size(); i++) {
					if (next().tryTransmitter(curTransmitter)) {
						break;
					}
				}
			}
		}
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).reschedule();
		}
		long period = (long) PredictOreKit.PREDICT_INTERVAL_SECONDS * 1000;
		LOG.info("observations scheduled. next update at: {}", new Date(clock.millis() + period));
		synchronized (this) {
			rescheduleThread.schedule(new SafeRunnable() {

				@Override
				public void safeRun() {
					LOG.info("reschedule observations");
					dao.reload();
					reschedule();
				}
			}, period, TimeUnit.MILLISECONDS);
		}
	}

	public ObservationRequest enableSatellite(Satellite satellite) {
		LOG.info("satellite {} enabled", satellite);
		ObservationRequest result = null;
		for (int i = 0; i < devices.size(); i++) {
			for (Transmitter cur : satellite.getTransmitters()) {
				ObservationRequest req = next().enableTransmitter(cur);
				// satellite might have several transmitters enabled
				// return observation for the first scheduled
				if (req != null) {
					result = req;
				}
			}
		}
		return result;
	}

	public void disableSatellite(Satellite satelliteToEdit) {
		LOG.info("satellite {} disabled. reschedule", satelliteToEdit.getId());
		for (int i = 0; i < devices.size(); i++) {
			for (Transmitter curTransmitter : satelliteToEdit.getTransmitters()) {
				devices.get(i).disableTransmitter(curTransmitter);
			}
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

	public List<ObservationRequest> startImmediately(Satellite satellite) {
		List<ObservationRequest> result = new ArrayList<>();
		for (int i = 0; i < devices.size(); i++) {
			for (Transmitter curTransmitter : satellite.getTransmitters()) {
				ObservationRequest cur = devices.get(i).startImmediately(curTransmitter);
				if (cur == null) {
					continue;
				}
				result.add(cur);
			}
		}
		return result;
	}

	public boolean completeImmediately(String observationId) {
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).completeImmediately(observationId)) {
				return true;
			}
		}
		return false;
	}

	public ObservationRequest findFirstByTransmitter(Transmitter transmitter) {
		for (int i = 0; i < devices.size(); i++) {
			ObservationRequest result = devices.get(i).findFirstByTransmitter(transmitter);
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

	public List<DeviceStatus> getStatus() {
		List<DeviceStatus> result = new ArrayList<>(devices.size());
		for (Device cur : devices) {
			result.add(cur.getStatus());
		}
		Collections.sort(result, DeviceStatusComparator.INSTANCE);
		return result;
	}

}

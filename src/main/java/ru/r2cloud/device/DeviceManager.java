package ru.r2cloud.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.DeviceStatusComparator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class DeviceManager implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);

	private final Configuration config;
	private final List<Device> devices = new ArrayList<>();
	private ScheduledExecutorService rescheduleThread = null;

	private int currentDevice = 0;

	public DeviceManager(Configuration config) {
		this.config = config;
	}

	public void addDevice(Device device) {
		this.devices.add(device);
	}

	@Override
	public void start() {
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).start();
		}
	}

	public void reschedule() {
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).reschedule();
		}
	}

	public ObservationRequest enable(Satellite satellite) {
		LOG.info("satellite {} enabled", satellite);
		ObservationRequest result = null;
		for (Transmitter cur : satellite.getTransmitters()) {
			for (int i = 0; i < devices.size(); i++) {
				ObservationRequest req = nextDevice().schedule(cur);
				// satellite might have several transmitters enabled
				// return observation for the first scheduled
				if (req != null) {
					if (result == null) {
						result = req;
					}
					break;
				}
			}
		}
		return result;
	}

	public ObservationRequest schedule(Satellite satellite) {
		LOG.info("scheduling satellite {}", satellite);
		ObservationRequest result = null;
		for (Transmitter cur : satellite.getTransmitters()) {
			for (int i = 0; i < devices.size(); i++) {
				Device nextDevice = nextDevice();
				// permanently assign transmitter to the device and schedule
				if (!nextDevice.tryTransmitter(cur)) {
					continue;
				}
				ObservationRequest req = nextDevice.schedule(cur);
				// satellite might have several transmitters enabled
				// return observation for the first scheduled
				if (req != null) {
					if (result == null) {
						result = req;
					}
					break;
				}
			}
		}
		return result;
	}

	public void schedule(List<Satellite> all) {
		for (Satellite cur : all) {
			for (Transmitter curTransmitter : cur.getTransmitters()) {
				for (int i = 0; i < devices.size(); i++) {
					if (nextDevice().tryTransmitter(curTransmitter)) {
						break;
					}
				}
			}
		}
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).reschedule();
		}
	}

	public void cancelObservations(Satellite satellite) {
		LOG.info("cancelling observations for satellite {}", satellite.getId());
		for (int i = 0; i < devices.size(); i++) {
			for (Transmitter curTransmitter : satellite.getTransmitters()) {
				if (devices.get(i).findById(curTransmitter.getId()) == null) {
					continue;
				}
				devices.get(i).reschedule();
			}
		}
	}

	private Device nextDevice() {
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

	public List<ObservationRequest> findScheduledObservations() {
		List<ObservationRequest> result = new ArrayList<>();
		for (int i = 0; i < devices.size(); i++) {
			result.addAll(devices.get(i).findScheduledObservations());
		}
		Collections.sort(result, ObservationRequestComparator.INSTANCE);
		return result;
	}

	@Override
	public void stop() {
		Util.shutdown(rescheduleThread, config.getThreadPoolShutdownMillis());
		for (int i = 0; i < devices.size(); i++) {
			devices.get(i).stop();
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

	public Device findDeviceById(String id) {
		if (id == null) {
			return null;
		}
		for (Device cur : devices) {
			if (cur.getId().equals(id)) {
				return cur;
			}
		}
		return null;
	}

	public Device findDeviceByHost(String host) {
		if (host == null) {
			return null;
		}
		for (Device cur : devices) {
			if (cur.getDeviceConfiguration().getHost() == null) {
				continue;
			}
			if (cur.getDeviceConfiguration().getHost().equals(host)) {
				return cur;
			}
		}
		return null;
	}

}

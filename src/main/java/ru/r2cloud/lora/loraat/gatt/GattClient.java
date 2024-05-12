package ru.r2cloud.lora.loraat.gatt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bluez.GattCharacteristic1;
import org.bluez.exceptions.BluezFailedException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Util;

public class GattClient implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(GattClient.class);
	public static final String LORA_SERVICE_UUID = "5ff92dc2-6c02-27af-4846-8747b8b1cfe6";
	public static final String LORA_START_RX_UUID = "bb2053d7-4ea9-de9f-1d40-db01f59d50ab";
	public static final String LORA_STOP_RX_UUID = "892b0a03-ab8e-83a4-6841-a977a2dd4036";
	public static final String LORA_FRAME_UUID = "3c37ae1b-427f-e6a8-d643-634d36afca72";

	private final Set<String> configuredDevices = new HashSet<>();
	private final String address;
	private final Clock clock;
	private final Map<String, BluetoothDevice> connectedDevices = new ConcurrentHashMap<>();
	private final Map<String, List<LoraFrame>> receivedFrames = new HashMap<>();

	private DeviceManager deviceManager;

	public GattClient(String address, Clock clock, long timeout) {
		this.address = address;
		this.clock = clock;
		MethodCall.setDefaultTimeout(timeout);
	}

	@Override
	public void start() {
		try {
			deviceManager = DeviceManager.createInstance(address);
			LOG.info("dbus connected");
			deviceManager.scanForBluetoothAdapters();

			AbstractPropertiesChangedHandler frameUpdate = new AbstractPropertiesChangedHandler() {
				@Override
				public void handle(Properties.PropertiesChanged propertiesChanged) {
					if (!propertiesChanged.getInterfaceName().equals(GattCharacteristic1.class.getName())) {
						return;
					}
					String lowerCasePath = propertiesChanged.getPath().toLowerCase(Locale.UK);
					// Check if notification from the registered device
					String bluetoothAddress = null;
					for (String cur : configuredDevices) {
						if (lowerCasePath.contains(cur.replace(':', '_'))) {
							bluetoothAddress = cur;
							break;
						}
					}
					if (bluetoothAddress == null) {
						// not for any registered device
						return;
					}

					Variant<?> rawValue = propertiesChanged.getPropertiesChanged().get("Value");
					if (rawValue == null || rawValue.getValue() == null) {
						// something else has changed
						return;
					}

					try {
						LoraFrame frame = LoraFrame.read((byte[]) rawValue.getValue());
						if (frame == null) {
							return;
						}
						LOG.info("[{}] received frame: {}", bluetoothAddress, frame);
						synchronized (receivedFrames) {
							List<LoraFrame> frames = receivedFrames.get(bluetoothAddress);
							if (frames == null) {
								frames = new ArrayList<>();
								receivedFrames.put(bluetoothAddress, frames);
							}
							frames.add(frame);
						}
					} catch (IOException e) {
						Util.logIOException(LOG, false, "[" + bluetoothAddress + "] can't read input", e);
						return;
					}
				}
			};
			// Can be deviceManager.registerPropertyHandler(frameUpdate);
			// but bluez-dbus is old and not compatible with new dbus-java-core
			deviceManager.getDbusConnection().addSigHandler(frameUpdate.getImplementationClass(), frameUpdate);

			Map<String, BluetoothDevice> available = new HashMap<>();
			for (BluetoothDevice cur : deviceManager.getDevices(true)) {
				available.put(cur.getAddress().toLowerCase(Locale.UK), cur);
			}
			for (String cur : configuredDevices) {
				if (!available.containsKey(cur)) {
					LOG.error("bluetooth device {} is not paired. Pair it first using bluetoothctl and then restart r2cloud", cur);
					continue;
				}
				connectedDevices.put(cur, available.get(cur));
			}

		} catch (Exception e) {
			LOG.error("unable to start Gatt client", e);
			stop();
		}
	}

	@Override
	public void stop() {
		if (deviceManager == null) {
			return;
		}
		deviceManager.closeConnection();
	}

	public LoraResponse startObservation(String bluetoothAddress, LoraObservationRequest loraRequest) {
		BluetoothDevice cur = connectedDevices.get(bluetoothAddress);
		if (cur == null) {
			return new LoraResponse("bluetooth is not paired or unknown");
		}
		if (!cur.isConnected()) {
			LOG.info("[{}] connecting..", bluetoothAddress);
			boolean connected = false;
			try {
				connected = cur.connect();
			} catch (Exception e) {
				return new LoraResponse("unable to connect: " + e.getMessage());
			}
			if (!connected) {
				return new LoraResponse("unable to connect");
			}
		}
		for (BluetoothGattService service : cur.getGattServices()) {
			if (!service.getUuid().equalsIgnoreCase(LORA_SERVICE_UUID)) {
				continue;
			}
			BluetoothGattCharacteristic frame = null;
			BluetoothGattCharacteristic startRx = null;
			for (BluetoothGattCharacteristic ch : service.getGattCharacteristics()) {
				if (ch.getUuid().equalsIgnoreCase(LORA_START_RX_UUID)) {
					startRx = ch;
				}
				if (ch.getUuid().equalsIgnoreCase(LORA_FRAME_UUID)) {
					frame = ch;
				}
			}
			if (startRx == null) {
				return new LoraResponse("can't find startRx characteristic");
			}
			if (frame == null) {
				return new LoraResponse("can't find frame characteristic");
			}
			synchronized (receivedFrames) {
				List<LoraFrame> previouslyReceived = receivedFrames.remove(bluetoothAddress);
				if (previouslyReceived != null && !previouslyReceived.isEmpty()) {
					LOG.info("[{}] previously received frames {}", bluetoothAddress, previouslyReceived.size());
				}
			}
			try {
				frame.startNotify();
				LOG.info("[{}] subscribed for frame notifications", bluetoothAddress);
			} catch (Exception e) {
				return new LoraResponse("unable to subscribe for updates: " + e.getMessage());
			}
			try {
				startRx.writeValue(loraRequest.toByteArray(clock.millis()), null);
			} catch (Exception e) {
				try {
					frame.stopNotify();
				} catch (BluezFailedException e1) {
					LOG.error("unable to stop notifications after failure", e);
				}
				return new LoraResponse("unable to start rx: " + e.getMessage());
			}
			LoraResponse result = new LoraResponse();
			result.setStatus(ResponseStatus.SUCCESS);
			return result;
		}
		return new LoraResponse("can't find lora service");
	}

	public LoraResponse stopObservation(String bluetoothAddress) {
		BluetoothDevice cur = connectedDevices.get(bluetoothAddress);
		if (cur == null) {
			return new LoraResponse("bluetooth is not paired or unknown");
		}
		if (!cur.isConnected()) {
			LOG.info("[{}] connecting..", bluetoothAddress);
			boolean connected = false;
			try {
				connected = cur.connect();
			} catch (Exception e) {
				return new LoraResponse("unable to connect: " + e.getMessage());
			}
			if (!connected) {
				return new LoraResponse("unable to connect");
			}
		}
		for (BluetoothGattService service : cur.getGattServices()) {
			if (!service.getUuid().equalsIgnoreCase(LORA_SERVICE_UUID)) {
				continue;
			}
			BluetoothGattCharacteristic frame = null;
			BluetoothGattCharacteristic stopRx = null;
			for (BluetoothGattCharacteristic ch : service.getGattCharacteristics()) {
				if (ch.getUuid().equalsIgnoreCase(LORA_STOP_RX_UUID)) {
					stopRx = ch;
				}
				if (ch.getUuid().equalsIgnoreCase(LORA_FRAME_UUID)) {
					frame = ch;
				}
			}
			if (stopRx != null) {
				try {
					stopRx.writeValue(new byte[0], null);
					LOG.info("[{}] lora rx stopped", bluetoothAddress);
				} catch (Exception e) {
					LOG.error("unable to stop lora rx", e);
				}
			}
			if (frame != null) {
				try {
					frame.stopNotify();
					LOG.info("[{}] unsubscribed from frame notifications", bluetoothAddress);
				} catch (BluezFailedException e) {
					LOG.error("unable to stop notifications", e);
				}
			}
			synchronized (receivedFrames) {
				List<LoraFrame> frames = receivedFrames.remove(bluetoothAddress);
				if (frames == null) {
					frames = new ArrayList<>();
				}
				LoraResponse result = new LoraResponse();
				result.setStatus(ResponseStatus.SUCCESS);
				result.setFrames(frames);
				return result;
			}
		}
		return new LoraResponse("can't find lora service");
	}

	public DeviceConnectionStatus getStatus(String bluetoothAddress) {
		BluetoothDevice cur = connectedDevices.get(bluetoothAddress);
		if (cur == null) {
			// most likely not paired
			return DeviceConnectionStatus.FAILED;
		}
		if (cur.isConnected()) {
			return DeviceConnectionStatus.CONNECTED;
		}
		LOG.info("[{}] connecting..", bluetoothAddress);
		try {
			if (cur.connect()) {
				return DeviceConnectionStatus.CONNECTED;
			}
		} catch (Exception e) {
			LOG.info("[{}] unable to connect: {}", bluetoothAddress, e.getMessage());
		}
		return DeviceConnectionStatus.FAILED;
	}

	public void addDevice(String bluetoothAddress) {
		this.configuredDevices.add(bluetoothAddress);
	}

	// used for testing
	Map<String, List<LoraFrame>> getReceivedFrames() {
		synchronized (receivedFrames) {
			return new HashMap<>(receivedFrames);
		}
	}

}

package ru.r2cloud.lora.loraat.gatt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bluez.GattCharacteristic1;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;

import ru.r2cloud.lora.LoraFrame;

public class MockGattServer {

	private static final String adapterPath = "/org/bluez/hci0";

	private final String remoteBluetoothAddress;
	private final BluezServer bluezServer;

	private MockBleCharacteristic startRx;
	private String frameObjectPath;

	public MockGattServer(String remoteBluetoothAddress, BluezServer bluezServer) {
		this.remoteBluetoothAddress = remoteBluetoothAddress;
		this.bluezServer = bluezServer;
	}

	public void start() throws Exception {
		String devicePath = adapterPath + "/dev_" + remoteBluetoothAddress.replace(':', '_').toUpperCase(Locale.UK);
		String servicePrefix = devicePath + "/service001";
		startRx = new MockBleCharacteristic(servicePrefix + "/char001", new String[] { "encrypted-write" }, GattClient.LORA_START_RX_UUID, servicePrefix, null, null);
		MockBleCharacteristic stopRx = new MockBleCharacteristic(servicePrefix + "/char002", new String[] { "encrypted-write" }, GattClient.LORA_STOP_RX_UUID, servicePrefix, null, null);
		frameObjectPath = servicePrefix + "/char003";
		MockBleCharacteristic frame = new MockBleCharacteristic(frameObjectPath, new String[] { "read" }, GattClient.LORA_FRAME_UUID, servicePrefix, null, null);

		List<BleCharacteristic> chars = new ArrayList<>();
		chars.add(startRx);
		chars.add(stopRx);
		chars.add(frame);

		bluezServer.registerBluetoothAdapter(new BluezAdapter(adapterPath, "d8:3a:dd:53:89:ec"));
		bluezServer.registerBluetoothDevice(new BluezDevice(devicePath, remoteBluetoothAddress, new BleService(servicePrefix, GattClient.LORA_SERVICE_UUID, true, chars)));
	}

	public void sendLoraFrame(LoraFrame frame) throws Exception {
		Map<String, Variant<?>> propsChanged = new HashMap<>();
		propsChanged.put("Value", new Variant<>(frame.write()));
		bluezServer.sendMessage(new PropertiesChanged(frameObjectPath.toUpperCase(Locale.UK), GattCharacteristic1.class.getName(), propsChanged, Collections.emptyList()));
	}

	public void awaitObservation() {
		startRx.awaitWritingData();
	}

}

package ru.r2cloud.lora.loraat.gatt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class ScheduleCharacteristic extends BleCharacteristic {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleCharacteristic.class);
	private static final int PROTOCOL_VERSION = 2;
	private final DeviceManager manager;
	private final Clock clock;

	public ScheduleCharacteristic(String objectPath, String[] flags, String uuId, String servicePath, BleDescriptor descriptor, DeviceManager manager, Clock clock) {
		super(objectPath, flags, uuId, servicePath, descriptor);
		this.manager = manager;
		this.clock = clock;
	}

	@Override
	public byte[] read(String bluetoothAddress) {
		LoraAtBleDevice device = getLoraDevice(bluetoothAddress);
		if (device == null) {
			return new byte[0];
		}
		List<ObservationRequest> requests = device.findScheduledObservations();
		if (requests.isEmpty()) {
			LOG.info("[{}] no scheduled observations", bluetoothAddress);
			return new byte[0];
		}
		Collections.sort(requests, ObservationRequestComparator.INSTANCE);
		long currentTime = clock.millis();
		ObservationRequest req = null;
		for (ObservationRequest cur : requests) {
			if (currentTime < cur.getEndTimeMillis()) {
				req = cur;
				break;
			}
		}
		if (req == null) {
			LOG.info("[{}] can't find first observation", bluetoothAddress);
			return new byte[0];
		}

		Transmitter transmitter = device.findById(req.getTransmitterId());
		if (transmitter == null) {
			LOG.error("[{}] can't find transmitter: {}", bluetoothAddress, req.getTransmitterId());
			return new byte[0];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeByte(PROTOCOL_VERSION);
			dos.writeLong(req.getStartTimeMillis());
			dos.writeLong(req.getEndTimeMillis());
			dos.writeLong(currentTime);
			dos.writeLong(req.getFrequency());
			dos.writeInt((int) transmitter.getLoraBandwidth());
			dos.writeByte(transmitter.getLoraSpreadFactor());
			dos.writeByte(transmitter.getLoraCodingRate());
			dos.writeByte(transmitter.getLoraSyncword());
			dos.writeByte(10); // power
			dos.writeShort(transmitter.getLoraPreambleLength());
			dos.writeByte((int) device.getDeviceConfiguration().getGain());
			dos.writeByte(transmitter.getLoraLdro());
			if (transmitter.isLoraCrc()) {
				dos.writeByte(1);
			} else {
				dos.writeByte(0);
			}
			if (transmitter.isLoraExplicitHeader()) {
				dos.writeByte(1);
			} else {
				dos.writeByte(0);
			}
			// lora packet size cannot be more than 255 bytes
			dos.writeByte(transmitter.getBeaconSizeBytes());
			dos.writeShort(240); // over current protection. not used for RX
			dos.writeByte(0); // pin for TX. not used in RX
		} catch (IOException e) {
			LOG.error("[{}] can't serialize output", bluetoothAddress, e);
			return new byte[0];
		}
		return baos.toByteArray();
	}

	@Override
	public void write(byte[] value, String bluetoothAddress) {
		LoraAtBleDevice device = getLoraDevice(bluetoothAddress);
		if (device == null) {
			return;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		try (DataInputStream dis = new DataInputStream(bais)) {
			LoraFrame frame = new LoraFrame();
			int protocolVersion = dis.readUnsignedByte();
			if (protocolVersion != PROTOCOL_VERSION) {
				LOG.error("[{}] invalid protocol version {}, expected {}", bluetoothAddress, protocolVersion, PROTOCOL_VERSION);
				return;
			}
			frame.setFrequencyError(dis.readInt());
			frame.setRssi(dis.readShort());
			frame.setSnr(dis.readFloat());
			frame.setTimestamp(dis.readLong());
			int dataLength = dis.readUnsignedByte();
			// max lora packet is 255 bytes
			if (dataLength > 255) {
				return;
			}
			byte[] data = new byte[dataLength];
			dis.readFully(data);
			frame.setData(data);
			LOG.info("[{}] received frame: {}", bluetoothAddress, frame);
			device.addFrame(frame);
		} catch (IOException e) {
			Util.logIOException(LOG, false, "[" + bluetoothAddress + "] can't read input", e);
			return;
		}
	}

	private LoraAtBleDevice getLoraDevice(String bluetoothAddress) {
		Device device = manager.findDeviceById(Configuration.LORA_AT_DEVICE_PREFIX + bluetoothAddress);
		if (device == null) {
			LOG.info("[{}] ble device is not configured", bluetoothAddress);
			return null;
		}
		if (!(device instanceof LoraAtBleDevice)) {
			LOG.info("not a lora-at device: {}", bluetoothAddress);
			return null;
		}
		return (LoraAtBleDevice) device;
	}

}

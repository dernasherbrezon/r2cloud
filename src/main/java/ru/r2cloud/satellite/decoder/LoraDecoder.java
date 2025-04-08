package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Util;

public class LoraDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(LoraDecoder.class);

	private final Class<? extends Beacon> beacon;

	public LoraDecoder(Class<? extends Beacon> beacon) {
		this.beacon = beacon;
	}

	@Override
	public DecoderResult decode(File rawFile, Observation request, final Transmitter transmitter, final Satellite satellite) {
		DecoderResult result = new DecoderResult();
		result.setRawPath(null);
		int numberOfDecodedPackets = 0;
		try (BeaconInputStream<? extends Beacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(rawFile)), beacon)) {
			long totalSize = 0;
			while (bis.hasNext()) {
				Beacon beacon = bis.next();
				numberOfDecodedPackets++;
				totalSize += beacon.getRawData().length;
			}
			result.setNumberOfDecodedPackets(numberOfDecodedPackets);
			result.setTotalSize(totalSize);

		} catch (IOException e) {
			LOG.error("unable to read lora beacons", e);
		}
		if (numberOfDecodedPackets <= 0) {
			Util.deleteQuietly(rawFile);
		} else {
			result.setDataPath(rawFile);
		}
		return result;
	}

	public Class<? extends Beacon> getBeacon() {
		return beacon;
	}

}

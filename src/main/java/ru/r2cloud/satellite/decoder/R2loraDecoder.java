package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.LoraBeacon;
import ru.r2cloud.model.ObservationRequest;

public class R2loraDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(R2loraDecoder.class);

	@Override
	public DecoderResult decode(File rawFile, ObservationRequest request) {
		DecoderResult result = new DecoderResult();
		result.setRawPath(null);
		try (BeaconInputStream<LoraBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(rawFile)), LoraBeacon.class)) {
			long numberOfDecodedPackets = 0;
			while (bis.hasNext()) {
				bis.next();
				numberOfDecodedPackets++;
			}
			result.setNumberOfDecodedPackets(numberOfDecodedPackets);
			result.setDataPath(rawFile);
		} catch (IOException e) {
			LOG.error("unable to read lora beacons", e);
		}
		return result;
	}

}

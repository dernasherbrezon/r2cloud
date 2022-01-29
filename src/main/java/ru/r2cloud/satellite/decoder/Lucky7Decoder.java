package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.lucky7.Lucky7;
import ru.r2cloud.jradio.lucky7.Lucky7Beacon;
import ru.r2cloud.jradio.lucky7.Lucky7PictureDecoder;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Lucky7Decoder extends TelemetryDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(Lucky7Decoder.class);

	public Lucky7Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public DecoderResult decode(File rawIq, ObservationRequest req) {
		DecoderResult result = super.decode(rawIq, req);
		if (result.getDataPath() != null) {
			List<Lucky7Beacon> beacons = new ArrayList<>();
			try (BeaconInputStream<Lucky7Beacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), Lucky7Beacon.class)) {
				while (bis.hasNext()) {
					beacons.add(bis.next());
				}
			} catch (IOException e) {
				LOG.error("unable to read data", e);
			}
			Lucky7PictureDecoder decoder = new Lucky7PictureDecoder(beacons);
			while (decoder.hasNext()) {
				File imageFile = saveImage("lucky7-" + req.getId() + ".jpg", decoder.next());
				if (imageFile != null) {
					result.setImagePath(imageFile);
					// interested only in the first image
					break;
				}
			}
		}
		return result;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		FskDemodulator demodulator = new FskDemodulator(source, baudRate, 5000.0f, Util.convertDecimation(baudRate), 1000);
		SoftToHard bs = new SoftToHard(demodulator);
		CorrelateSyncword correlate = new CorrelateSyncword(bs, 3, "0010110111010100", 37 * 8);
		return new Lucky7(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Lucky7Beacon.class;
	}
}
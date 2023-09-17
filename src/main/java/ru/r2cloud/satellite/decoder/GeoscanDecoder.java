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
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.geoscan.Geoscan;
import ru.r2cloud.jradio.geoscan.GeoscanBeacon;
import ru.r2cloud.jradio.geoscan.GeoscanPictureDecoder;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class GeoscanDecoder extends TelemetryDecoder {
	
	private static final Logger LOG = LoggerFactory.getLogger(GeoscanDecoder.class);

	public GeoscanDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}
	
	@Override
	public DecoderResult decode(File rawIq, Observation req, final Transmitter transmitter) {
		DecoderResult result = super.decode(rawIq, req, transmitter);
		if (result.getDataPath() != null) {
			List<GeoscanBeacon> beacons = new ArrayList<>();
			try (BeaconInputStream<GeoscanBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), GeoscanBeacon.class)) {
				while (bis.hasNext()) {
					beacons.add(bis.next());
				}
			} catch (IOException e) {
				LOG.error("unable to read data", e);
			}
			GeoscanPictureDecoder decoder = new GeoscanPictureDecoder(beacons);
			while (decoder.hasNext()) {
				File imageFile = saveImage("geoscan-" + req.getId() + ".jpg", decoder.next());
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
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, Observation req) {
		return new Geoscan(source);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return GeoscanBeacon.class;
	}

}

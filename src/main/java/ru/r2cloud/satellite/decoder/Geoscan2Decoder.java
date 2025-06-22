package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.geoscan.Geoscan;
import ru.r2cloud.jradio.geoscan.Geoscan2Beacon;
import ru.r2cloud.jradio.geoscan.Geoscan2PictureDecoder;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Geoscan2Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;
	private final int beaconSizeBytes;

	public Geoscan2Decoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, int beaconSizeBytes) {
		super(predict, config);
		this.beacon = beacon;
		this.beaconSizeBytes = beaconSizeBytes;
	}

	@Override
	protected List<Instrument> decodeImage(Satellite satellite, List<? extends Beacon> beacons) {
		Instrument camera = satellite.findFirstSeries();
		if (camera == null) {
			return Collections.emptyList();
		}
		List<File> series = new ArrayList<>();
		int index = 0;
		@SuppressWarnings("unchecked")
		Geoscan2PictureDecoder decoder = new Geoscan2PictureDecoder((List<Geoscan2Beacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("geoscan-" + index + ".jpg", image);
			if (imageFile == null) {
				continue;
			}
			series.add(imageFile);
		}
		if (series.isEmpty()) {
			return Collections.emptyList();
		}
		Instrument result = new Instrument(camera);
		result.setImageSeries(series);
		return Collections.singletonList(result);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, Observation req) {
		return new Geoscan<>(source, beacon, beaconSizeBytes);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}

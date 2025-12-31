package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.lucky7.Lucky7;
import ru.r2cloud.jradio.lucky7.Lucky7Beacon;
import ru.r2cloud.jradio.lucky7.Lucky7PictureDecoder;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Lucky7Decoder extends TelemetryDecoder {

	public Lucky7Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
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
		Lucky7PictureDecoder decoder = new Lucky7PictureDecoder((List<Lucky7Beacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("lucky7-" + index + ".jpg", image);
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
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		CorrelateSyncword correlate = new CorrelateSyncword(demodulator, 3, "0010110111010100", 37 * 8);
		return new Lucky7(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Lucky7Beacon.class;
	}
}
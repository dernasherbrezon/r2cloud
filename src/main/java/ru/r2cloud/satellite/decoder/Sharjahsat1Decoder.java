package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.sharjahsat.Sharjahsat1Beacon;
import ru.r2cloud.jradio.sharjahsat.Sharjahsat1PictureDecoder;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Sharjahsat1Decoder extends Ax25G3ruhDecoder {

	public Sharjahsat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config, Sharjahsat1Beacon.class, null);
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
		Sharjahsat1PictureDecoder decoder = new Sharjahsat1PictureDecoder((List<Sharjahsat1Beacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("sharjahsat1-" + index + ".jpg", image);
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

}

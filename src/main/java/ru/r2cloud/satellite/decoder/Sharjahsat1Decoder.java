package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.sharjahsat.Sharjahsat1Beacon;
import ru.r2cloud.jradio.sharjahsat.Sharjahsat1PictureDecoder;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Sharjahsat1Decoder extends Ax25G3ruhDecoder {

	public Sharjahsat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config, Sharjahsat1Beacon.class, null);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		Sharjahsat1PictureDecoder decoder = new Sharjahsat1PictureDecoder((List<Sharjahsat1Beacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage result = decoder.next();
			if (result == null) {
				continue;
			}
			return result;
		}
		return null;
	}
}

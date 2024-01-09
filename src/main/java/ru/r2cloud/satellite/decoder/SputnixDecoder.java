package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.sputnix.SputnixBeacon;
import ru.r2cloud.jradio.sputnix.SputnixPictureDecoder;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class SputnixDecoder extends UspDecoder {

	public SputnixDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config, SputnixBeacon.class);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		SputnixPictureDecoder decoder = new SputnixPictureDecoder((List<SputnixBeacon>) beacons);
		while (decoder.hasNext()) {
			return decoder.next();
		}
		return null;
	}

}

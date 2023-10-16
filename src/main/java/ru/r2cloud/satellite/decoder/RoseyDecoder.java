package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.roseycub.RoseyCubesatBeacon;
import ru.r2cloud.jradio.roseycub.RoseyPictureDecoder;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class RoseyDecoder extends Ax25Decoder {

	public RoseyDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config, RoseyCubesatBeacon.class, null);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		RoseyPictureDecoder decoder = new RoseyPictureDecoder((List<RoseyCubesatBeacon>) beacons);
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

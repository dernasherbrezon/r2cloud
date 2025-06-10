package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.roseycub.RoseyCubesatBeacon;
import ru.r2cloud.jradio.roseycub.RoseyPictureDecoder;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class RoseyDecoder extends Ax25Decoder {

	public RoseyDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config, RoseyCubesatBeacon.class, null);
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
		RoseyPictureDecoder decoder = new RoseyPictureDecoder((List<RoseyCubesatBeacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("rosey-" + index + ".jpg", image);
			if (imageFile == null) {
				continue;
			}
			series.add(imageFile);
		}
		Instrument result = new Instrument(camera);
		result.setImageSeries(series);
		return Collections.singletonList(result);
	}

}

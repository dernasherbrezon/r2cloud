package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.il2p.Il2pBeacon;
import ru.r2cloud.jradio.silversat.SilversatPacketSource;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvImage;
import ru.r2cloud.util.Configuration;

public class SilversatDecoder extends Il2pDecoder {

	public SilversatDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, int beaconSize) {
		super(predict, config, beacon, beaconSize);
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
		SsdvDecoder decoder = new SsdvDecoder(new SilversatPacketSource(((List<Il2pBeacon>) beacons).iterator()));
		while (decoder.hasNext()) {
			SsdvImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("silversat-" + index + ".jpg", image.getImage());
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

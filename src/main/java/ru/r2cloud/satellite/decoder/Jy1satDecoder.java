package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.jy1sat.Jy1sat;
import ru.r2cloud.jradio.jy1sat.Jy1satBeacon;
import ru.r2cloud.jradio.jy1sat.Jy1satSsdvPacketSource;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvImage;
import ru.r2cloud.util.Configuration;

public class Jy1satDecoder extends TelemetryDecoder {

	public Jy1satDecoder(PredictOreKit predict, Configuration config) {
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
		SsdvDecoder decoder = new SsdvDecoder(new Jy1satSsdvPacketSource(((List<Jy1satBeacon>) beacons).iterator()));
		while (decoder.hasNext()) {
			SsdvImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("jy1sat-" + index + ".jpg", image.getImage());
			if (imageFile == null) {
				continue;
			}
			series.add(imageFile);
		}
		Instrument result = new Instrument(camera);
		result.setImageSeries(series);
		return Collections.singletonList(result);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Jy1sat(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Jy1satBeacon.class;
	}
}

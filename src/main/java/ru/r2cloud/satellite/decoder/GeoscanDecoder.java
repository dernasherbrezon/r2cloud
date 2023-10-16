package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.geoscan.Geoscan;
import ru.r2cloud.jradio.geoscan.GeoscanBeacon;
import ru.r2cloud.jradio.geoscan.GeoscanPictureDecoder;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class GeoscanDecoder extends TelemetryDecoder {

	public GeoscanDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		GeoscanPictureDecoder decoder = new GeoscanPictureDecoder((List<GeoscanBeacon>) beacons);
		while (decoder.hasNext()) {
			return decoder.next();
		}
		return null;
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

package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.geoscan.Geoscan;
import ru.r2cloud.jradio.geoscan.GeoscanBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class GeoscanDecoder extends TelemetryDecoder {

	public GeoscanDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, ObservationRequest req) {
		return new Geoscan(source);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return GeoscanBeacon.class;
	}

}

package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.jy1sat.Jy1sat;
import ru.r2cloud.jradio.jy1sat.Jy1satBeacon;
import ru.r2cloud.jradio.jy1sat.Jy1satSsdvPacketSource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvImage;
import ru.r2cloud.util.Configuration;

public class Jy1satDecoder extends TelemetryDecoder {

	public Jy1satDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		SsdvDecoder decoder = new SsdvDecoder(new Jy1satSsdvPacketSource(((List<Jy1satBeacon>) beacons).iterator()));
		while (decoder.hasNext()) {
			SsdvImage cur = decoder.next();
			if (cur == null) {
				continue;
			}
			BufferedImage result = cur.getImage();
			if (result == null) {
				continue;
			}
			return result;
		}
		return null;
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

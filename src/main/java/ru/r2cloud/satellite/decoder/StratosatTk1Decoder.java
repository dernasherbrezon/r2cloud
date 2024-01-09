package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.sstk1.StratosatTk1;
import ru.r2cloud.jradio.sstk1.StratosatTk1Beacon;
import ru.r2cloud.jradio.sstk1.StratosatTk1PictureDecoder;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class StratosatTk1Decoder extends TelemetryDecoder {

	public StratosatTk1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}
	
	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		StratosatTk1PictureDecoder decoder = new StratosatTk1PictureDecoder((List<StratosatTk1Beacon>) beacons);
		while (decoder.hasNext()) {
			return decoder.next();
		}
		return null;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new StratosatTk1(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return StratosatTk1Beacon.class;
	}
}

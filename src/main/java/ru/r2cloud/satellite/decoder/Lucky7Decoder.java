package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.lucky7.Lucky7;
import ru.r2cloud.jradio.lucky7.Lucky7Beacon;
import ru.r2cloud.jradio.lucky7.Lucky7PictureDecoder;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Lucky7Decoder extends TelemetryDecoder {

	public Lucky7Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
		@SuppressWarnings("unchecked")
		Lucky7PictureDecoder decoder = new Lucky7PictureDecoder((List<Lucky7Beacon>) beacons);
		while (decoder.hasNext()) {
			BufferedImage result = decoder.next();
			if (result == null) {
				continue;
			}
			return result;
		}
		return null;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		SoftToHard bs = new SoftToHard(demodulator);
		CorrelateSyncword correlate = new CorrelateSyncword(bs, 3, "0010110111010100", 37 * 8);
		return new Lucky7(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Lucky7Beacon.class;
	}
}
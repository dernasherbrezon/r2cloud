package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.gaspacs.Gaspacs;
import ru.r2cloud.jradio.gaspacs.GaspacsBeacon;
import ru.r2cloud.jradio.gaspacs.GaspacsPacketSource;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvImage;
import ru.r2cloud.ssdv.SsdvPacket;
import ru.r2cloud.util.Configuration;

public class GaspacsDecoder extends TelemetryDecoder {

	public GaspacsDecoder(PredictOreKit predict, Configuration config) {
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
		// real-world packets received from space can be unsorted for some reason
		// extract them into List for SsdvDecoder to sort and produce better picture
		List<SsdvPacket> packets = new ArrayList<>();
		@SuppressWarnings("unchecked")
		GaspacsPacketSource source = new GaspacsPacketSource(((List<GaspacsBeacon>) beacons).iterator());
		while (source.hasNext()) {
			packets.add(source.next());
		}
		SsdvDecoder decoder = new SsdvDecoder(packets);
		while (decoder.hasNext()) {
			SsdvImage image = decoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("gaspacs-" + index + ".jpg", image.getImage());
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
		return new Gaspacs(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return GaspacsBeacon.class;
	}

}

package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.gaspacs.Gaspacs;
import ru.r2cloud.jradio.gaspacs.GaspacsBeacon;
import ru.r2cloud.jradio.gaspacs.GaspacsPacketSource;
import ru.r2cloud.model.Observation;
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
	protected BufferedImage decodeImage(List<? extends Beacon> beacons) {
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
			SsdvImage next = decoder.next();
			if (next == null) {
				continue;
			}
			BufferedImage cur = next.getImage();
			if (cur == null) {
				continue;
			}
			return cur;
		}
		return null;
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

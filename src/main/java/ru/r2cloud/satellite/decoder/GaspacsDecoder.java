package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.gaspacs.Gaspacs;
import ru.r2cloud.jradio.gaspacs.GaspacsBeacon;
import ru.r2cloud.jradio.gaspacs.GaspacsPacketSource;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvPacket;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class GaspacsDecoder extends TelemetryDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(GaspacsDecoder.class);

	public GaspacsDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public DecoderResult decode(File rawIq, ObservationRequest req) {
		DecoderResult result = super.decode(rawIq, req);
		if (result.getDataPath() != null) {
			List<GaspacsBeacon> beacons = new ArrayList<>();
			try (BeaconInputStream<GaspacsBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), GaspacsBeacon.class)) {
				while (bis.hasNext()) {
					beacons.add(bis.next());
				}
			} catch (IOException e) {
				LOG.error("unable to read data", e);
				return result;
			}
			// real-world packets received from space can be unsorted for some reason
			// extract them into List for SsdvDecoder to sort and produce better picture
			List<SsdvPacket> packets = new ArrayList<>();
			GaspacsPacketSource source = new GaspacsPacketSource(beacons.iterator());
			while (source.hasNext()) {
				packets.add(source.next());
			}
			SsdvDecoder decoder = new SsdvDecoder(packets);
			while (decoder.hasNext()) {
				File imageFile = saveImage("ssdv-" + req.getId() + ".jpg", decoder.next().getImage());
				if (imageFile != null) {
					result.setImagePath(imageFile);
					// interested only in the first image
					break;
				}
			}
		}
		return result;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		FskDemodulator demod = new FskDemodulator(source, baudRate, 2400.0f, Util.convertDecimation(baudRate), 2000, false);
		return new Gaspacs(demod);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return GaspacsBeacon.class;
	}

}

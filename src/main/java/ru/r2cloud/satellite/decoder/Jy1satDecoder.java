package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.jy1sat.Jy1sat;
import ru.r2cloud.jradio.jy1sat.Jy1satBeacon;
import ru.r2cloud.jradio.jy1sat.Jy1satSsdvPacketSource;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Jy1satDecoder extends TelemetryDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(Jy1satDecoder.class);

	public Jy1satDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public DecoderResult decode(File rawIq, ObservationRequest req) {
		DecoderResult result = super.decode(rawIq, req);
		if (result.getDataPath() != null) {
			try (BeaconInputStream<Jy1satBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), Jy1satBeacon.class)) {
				SsdvDecoder decoder = new SsdvDecoder(new Jy1satSsdvPacketSource(bis));
				while (decoder.hasNext()) {
					File imageFile = saveImage("ssdv-" + req.getId() + ".jpg", decoder.next().getImage());
					if (imageFile != null) {
						result.setImagePath(imageFile);
						// interested only in the first image
						break;
					}
				}
			} catch (IOException e) {
				LOG.error("unable to read data", e);
			}
		}
		return result;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		BpskDemodulator bpsk = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), 0.0, true);
		return new Jy1sat(bpsk);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Jy1satBeacon.class;
	}
}

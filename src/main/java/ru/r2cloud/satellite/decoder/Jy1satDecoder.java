package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

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
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.ssdv.SsdvDecoder;
import ru.r2cloud.ssdv.SsdvImage;
import ru.r2cloud.util.Configuration;

public class Jy1satDecoder extends TelemetryDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(Jy1satDecoder.class);

	public Jy1satDecoder(Configuration config) {
		super(config);
	}

	@Override
	public ObservationResult decode(File rawIq, ObservationRequest req) {
		ObservationResult result = super.decode(rawIq, req);
		if (result.getDataPath() != null) {
			try (BeaconInputStream<Jy1satBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), Jy1satBeacon.class)) {
				SsdvDecoder decoder = new SsdvDecoder(new Jy1satSsdvPacketSource(bis));
				while (decoder.hasNext()) {
					SsdvImage image = decoder.next();
					File imageFile = new File(config.getTempDirectory(), "ssdv-" + req.getId() + ".jpg");
					try {
						ImageIO.write(image.getImage(), "jpg", imageFile);
						result.setaPath(imageFile);
						// interested only in the first image
						break;
					} catch (IOException e) {
						LOG.error("unable to write image", e);
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
		BpskDemodulator bpsk = new BpskDemodulator(source, 1200, 5, 0.0, 2000.0f, true);
		return new Jy1sat(bpsk);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Jy1satBeacon.class;
	}
}

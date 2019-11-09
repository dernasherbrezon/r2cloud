package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public abstract class TelemetryDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(TelemetryDecoder.class);

	private final Configuration config;

	public TelemetryDecoder(Configuration config) {
		this.config = config;
	}

	@Override
	public ObservationResult decode(File rawIq, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setIqPath(rawIq);

		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), req.getId() + ".bin");
		BeaconSource<? extends Beacon> input = null;
		BeaconOutputStream aos = null;
		try {
			DopplerCorrectedSource source = new DopplerCorrectedSource(rawIq, req);
			input = createBeaconSource(source, req);
			aos = new BeaconOutputStream(new FileOutputStream(binFile));
			while (input.hasNext()) {
				Beacon next = input.next();
				next.setBeginMillis(req.getStartTimeMillis() + (long) ((next.getBeginSample() * 1000) / source.getContext().getSampleRate()));
				aos.write(next);
				numberOfDecodedPackets++;
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + rawIq, e);
			return result;
		} finally {
			Util.closeQuietly(input);
			Util.closeQuietly(aos);
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			if (binFile.exists() && !binFile.delete()) {
				LOG.error("unable to delete temp file: {}", binFile.getAbsolutePath());
			}
		} else {
			result.setDataPath(binFile);
		}
		return result;
	}

	public abstract BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req);

}

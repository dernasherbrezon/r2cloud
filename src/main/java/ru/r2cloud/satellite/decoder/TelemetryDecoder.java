package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public abstract class TelemetryDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(TelemetryDecoder.class);

	protected final Configuration config;
	protected final PredictOreKit predict;

	public TelemetryDecoder(PredictOreKit predict, Configuration config) {
		this.config = config;
		this.predict = predict;
	}

	@Override
	public DecoderResult decode(File rawIq, ObservationRequest req, final Transmitter transmitter) {
		DecoderResult result = new DecoderResult();
		result.setRawPath(rawIq);
		if (transmitter.getBaudRates() == null || transmitter.getBaudRates().isEmpty()) {
			LOG.error("[{}] baud rate is missing: {}", req.getId(), req.getSatelliteId());
			return result;
		}

		long numberOfDecodedPackets = 0;
		float sampleRate = transmitter.getInputSampleRate();
		File binFile = new File(config.getTempDirectory(), req.getId() + ".bin");
		List<BeaconSource<? extends Beacon>> input = null;
		try (BeaconOutputStream aos = new BeaconOutputStream(new FileOutputStream(binFile))) {
			input = createBeaconSources(rawIq, req, transmitter);
			for (int i = 0; i < input.size(); i++) {
				if (Thread.currentThread().isInterrupted()) {
					LOG.info("decoding thread interrupted. stopping...");
					break;
				}
				// process each beaconsource
				BeaconSource<? extends Beacon> currentInput = input.get(i);
				try {
					while (currentInput.hasNext()) {
						Beacon next = currentInput.next();
						next.setBeginMillis(req.getStartTimeMillis() + (long) ((next.getBeginSample() * 1000) / sampleRate));
						aos.write(next);
						numberOfDecodedPackets++;
					}
				} finally {
					Util.closeQuietly(currentInput);
				}
			}
		} catch (Exception e) {
			LOG.error("unable to process: {}", rawIq, e);
			return result;
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			Util.deleteQuietly(binFile);
		} else {
			result.setDataPath(binFile);
		}
		return result;
	}

	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req, final Transmitter transmitter) throws IOException {
		DemodulatorType type = config.getDemodulatorType(transmitter.getModulation());
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>(transmitter.getBaudRates().size());
		switch (type) {
		case JRADIO:
			for (Integer cur : transmitter.getBaudRates()) {
				ByteInput demodulator = createDemodulator(new DopplerCorrectedSource(predict, rawIq, req, transmitter), transmitter, cur);
				result.add(createBeaconSource(demodulator, req));
			}			
			break;
		default:
			LOG.error("unknown demodulator type: " + type);
			return Collections.emptyList();
		}
		if (result.isEmpty()) {
			throw new IllegalArgumentException("at least one beacon source should be specified");
		}
		return result;
	}

	private static ByteInput createDemodulator(FloatInput source, Transmitter transmitter, int baudRate) {
		switch (transmitter.getModulation()) {
		case GFSK:
			return new FskDemodulator(source, baudRate, transmitter.getDeviation(), Util.convertDecimation(baudRate), transmitter.getTransitionWidth(), true);
		case AFSK:
			return new AfskDemodulator(source, baudRate, transmitter.getDeviation(), transmitter.getAfCarrier(), Util.convertDecimation(baudRate));
		case BPSK:
			return new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), transmitter.getBpskCenterFrequency(), transmitter.isBpskDifferential());
		default:
			throw new RuntimeException("unknown jradio modulator: " + transmitter.getModulation());
		}
	}

	protected File saveImage(String path, BufferedImage image) {
		if (image == null) {
			return null;
		}
		File imageFile = new File(config.getTempDirectory(), path);
		try {
			ImageIO.write(image, "jpg", imageFile);
			return imageFile;
		} catch (IOException e) {
			LOG.error("unable to write image", e);
			return null;
		}
	}

	@SuppressWarnings("unused")
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, ObservationRequest req) {
		return null;
	}

	public abstract Class<? extends Beacon> getBeaconClass();

}

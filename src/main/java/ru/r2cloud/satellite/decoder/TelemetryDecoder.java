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
import ru.r2cloud.jradio.RxMetadata;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.sink.SnrCalculator;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.sdrmodem.SdrModemClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public abstract class TelemetryDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(TelemetryDecoder.class);

	protected final Configuration config;
	protected final PredictOreKit predict;
	private final boolean calculateSnr;

	public TelemetryDecoder(PredictOreKit predict, Configuration config) {
		this.config = config;
		this.predict = predict;
		this.calculateSnr = config.getBoolean("satellites.snr");
	}

	@Override
	public DecoderResult decode(File rawIq, Observation req, final Transmitter transmitter, final Satellite satellite) {
		DecoderResult result = new DecoderResult();
		result.setIq(rawIq);
		if (transmitter.getBaudRates() == null || transmitter.getBaudRates().isEmpty()) {
			LOG.error("[{}] baud rate is missing: {}", req.getId(), req.getSatelliteId());
			return result;
		}

		long totalSize = 0;
		float sampleRate = req.getSampleRate();
		List<Beacon> allBeacons = new ArrayList<>();
		for (Integer baudRate : transmitter.getBaudRates()) {
			try {
				List<BeaconSource<? extends Beacon>> input = createBeaconSources(rawIq, req, transmitter, baudRate);
				for (int i = 0; i < input.size(); i++) {
					if (Thread.currentThread().isInterrupted()) {
						LOG.info("decoding thread interrupted. stopping...");
						break;
					}
					List<Beacon> beacons = new ArrayList<>();
					// process each beaconsource
					BeaconSource<? extends Beacon> currentInput = input.get(i);
					try {
						while (currentInput.hasNext()) {
							Beacon next = currentInput.next();
							next.setBeginMillis(req.getStartTimeMillis() + (long) ((next.getBeginSample() * 1000) / sampleRate));
							RxMetadata meta = new RxMetadata();
							meta.setBaud(baudRate);
							next.setRxMeta(meta);
							beacons.add(next);
							totalSize += next.getRawData().length;
						}
					} finally {
						Util.closeQuietly(currentInput);
					}
					if (calculateSnr && !beacons.isEmpty()) {
						try (FloatInput next = new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate, true)) {
							SnrCalculator.enrichSnr(next, beacons, transmitter.getBandwidth(), 1);
						}
					}
					allBeacons.addAll(beacons);
				}
			} catch (Exception e) {
				LOG.error("[{}] unable to process baud rate {}", req.getId(), baudRate, e);
			}
		}
		if (satellite.getInstruments() != null) {
			result.setInstruments(decodeImage(satellite, allBeacons));
		}
		result.setNumberOfDecodedPackets(allBeacons.size());
		result.setTotalSize(totalSize);
		if (allBeacons.isEmpty()) {
			return result;
		}

		File binFile = new File(config.getTempDirectory(), req.getId() + ".bin");
		try (BeaconOutputStream aos = new BeaconOutputStream(new FileOutputStream(binFile))) {
			for (Beacon cur : allBeacons) {
				aos.write(cur);
			}
		} catch (Exception e) {
			LOG.error("[{}] unable to process: {}", req.getId(), rawIq, e);
			return result;
		}
		result.setData(binFile);
		return result;
	}

	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, Observation req, final Transmitter transmitter, Integer baudRate) throws IOException {
		DemodulatorType type = config.getDemodulatorType(transmitter.getModulation());
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>(transmitter.getBaudRates().size());
		switch (type) {
		case JRADIO:
			ByteInput demodulator = createDemodulator(new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate), transmitter, baudRate);
			result.add(createBeaconSource(demodulator, req));
			break;
		case SDRMODEM:
			demodulator = new SdrModemClient(config, rawIq, req, transmitter, baudRate);
			result.add(createBeaconSource(demodulator, req));
			break;
		case FILE:
			result.add(new BeaconInputStreamSource<>(rawIq, transmitter.getBeaconClass()));
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
		int decimation = (int) (source.getContext().getSampleRate() / Util.getSymbolSyncInput(baudRate, (long) source.getContext().getSampleRate()));
		switch (transmitter.getModulation()) {
		case GFSK:
			return new FskDemodulator(source, baudRate, transmitter.getDeviation(), decimation, transmitter.getTransitionWidth(), true);
		case AFSK:
			return new AfskDemodulator(source, baudRate, transmitter.getDeviation(), transmitter.getAfCarrier(), decimation);
		case BPSK:
			return new BpskDemodulator(source, baudRate, decimation, transmitter.getBpskCenterFrequency(), transmitter.isBpskDifferential());
		default:
			throw new RuntimeException("unknown jradio modulator: " + transmitter.getModulation());
		}
	}

	@SuppressWarnings("unused")
	protected List<Instrument> decodeImage(Satellite satellite, List<? extends Beacon> beacons) {
		return Collections.emptyList();
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
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, Observation req) {
		return null;
	}

	public abstract Class<? extends Beacon> getBeaconClass();

}

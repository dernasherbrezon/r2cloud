package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class SatdumpDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(SatdumpDecoder.class);

	private final ProcessFactory factory;
	private final Configuration config;

	public SatdumpDecoder(Configuration config, ProcessFactory factory) {
		this.factory = factory;
		this.config = config;
	}

	@Override
	public DecoderResult decode(File rawFile, Observation request, final Transmitter transmitter) {
		DecoderResult result = new DecoderResult();
		result.setRawPath(null);
		if (rawFile.length() == 0) {
			return result;
		}
		if (transmitter.getBeaconSizeBytes() > 0) {
			long numberOfDecodedPackets = rawFile.length() / transmitter.getBeaconSizeBytes();
			if (rawFile.length() % transmitter.getBeaconSizeBytes() != 0) {
				LOG.warn("[{}] unexpected number of bytes in the .cadu file. number of packets is incorrect", request.getId());
			}
			result.setNumberOfDecodedPackets(numberOfDecodedPackets);
			result.setTotalSize(rawFile.length());
			if (numberOfDecodedPackets > 0) {
				result.setImagePath(decodeImage(rawFile, request, transmitter));
			}
			if (numberOfDecodedPackets <= 0) {
				Util.deleteQuietly(rawFile);
			} else {
				result.setDataPath(rawFile);
			}
		} else {
			// FIXME test analog images
			if (rawFile.length() > 0) {
				result.setImagePath(decodeImage(rawFile, request, transmitter));
			} else {
				Util.deleteQuietly(rawFile);
			}
		}

		return result;
	}

	private File decodeImage(File rawFile, Observation request, final Transmitter transmitter) {
		File outputDirectory = new File(config.getTempDirectory(), request.getId());
		ProcessWrapper process = null;
		String commandLine = config.getProperty("satellites.satdump.path") + " " + transmitter.getSatdumpPipeline() + " cadu " + rawFile.getAbsolutePath() + " " + rawFile.getParentFile().getAbsolutePath() + " --tle_override explicitly_missing ";
		try {
			process = factory.create(commandLine, Redirect.INHERIT, true);
			int responseCode = process.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code from satdump: {}", request.getId(), responseCode);
				Util.deleteDirectory(outputDirectory.toPath());
			} else {
				LOG.info("[{}] satdump stopped: {}", request.getId(), responseCode);
			}
			// FIXME search for the image file
			return null;
		} catch (IOException e) {
			LOG.error("[{}] unable to decode", request.getId(), e);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}

	}

}

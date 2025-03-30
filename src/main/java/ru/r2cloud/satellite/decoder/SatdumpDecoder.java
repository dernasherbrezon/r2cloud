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
		if (!rawFile.exists() || rawFile.length() == 0) {
			return result;
		}
		result.setRawPath(rawFile);
		ProcessWrapper process = null;
		String commandLine = config.getProperty("satellites.satdump.path") + " " + transmitter.getSatdumpPipeline() + " baseband " + rawFile.getAbsolutePath() + " " + rawFile.getParentFile().getAbsolutePath() + " --tle_override explicitly_missing --samplerate " + request.getSampleRate()
				+ " --baseband_format ziq";
		try {
			process = factory.create(commandLine, Redirect.INHERIT, true);
			int responseCode = process.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code from satdump: {}", request.getId(), responseCode);
			} else {
				LOG.info("[{}] satdump stopped: {}", request.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to decode", request.getId(), e);
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return result;
		}
		if (transmitter.getBeaconSizeBytes() > 0) {
			int numberOfDecodedPackets = 0;
			File data = find(rawFile.getParentFile().listFiles(), ".cadu");
			if (data != null) {
				numberOfDecodedPackets = (int) (data.length() / transmitter.getBeaconSizeBytes());
				if (data.length() % transmitter.getBeaconSizeBytes() != 0) {
					LOG.warn("[{}] unexpected number of bytes in the data file. number of packets is incorrect", request.getId());
				}
			}
			result.setNumberOfDecodedPackets(numberOfDecodedPackets);
			result.setTotalSize(data.length());
			if (numberOfDecodedPackets > 0) {
				result.setDataPath(data);
			}
			if (numberOfDecodedPackets > 0) {
				// FIXME search for images and attach to the result
			}
		} else {
			// FIXME test analog images
//				result.setImagePath(decodeImage(rawFile, request, transmitter));
		}

		return result;
	}

	private static File find(File[] files, String extension) {
		for (File cur : files) {
			if (cur.getName().endsWith(extension)) {
				return cur;
			}
		}
		return null;
	}

}

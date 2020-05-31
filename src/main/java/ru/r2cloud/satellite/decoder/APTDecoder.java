package ru.r2cloud.satellite.decoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class APTDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(APTDecoder.class);

	private final ProcessFactory factory;
	private final Configuration config;

	public APTDecoder(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public DecoderResult decode(final File wavFile, final ObservationRequest request) {
		DecoderResult result = new DecoderResult();
		result.setRawPath(wavFile);
		File image = new File(config.getTempDirectory(), "apt-" + request.getId() + ".jpg");
		ProcessWrapper process = null;
		try {
			process = factory.create(config.getProperty("satellites.wxtoimg.path") + " -e HVC -t n -c -o " + wavFile.getAbsolutePath() + " " + image.getAbsolutePath(), true, false);
			final InputStream is = process.getInputStream();
			final List<String> lines = new ArrayList<>();
			Thread tis = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
						String curLine = null;
						while ((curLine = r.readLine()) != null) {
							synchronized (lines) {
								LOG.info(curLine);
								lines.add(curLine);
							}
						}
						r.close();
					} catch (Exception e) {
						LOG.error("unable to read input: {}", wavFile.getAbsolutePath(), e);
					}
				}
			}, "wxtoimg-daemon");
			tis.setDaemon(true);
			tis.start();
			process.waitFor();
			if (convert(result, lines)) {
				result.setaPath(image);
			} else {
				Util.deleteQuietly(image);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		}
		return result;
	}

	// success
	// Satellite: NOAA
	// Status: signal processing............................
	// Gain: 6.2
	// Channel A: 2 (near infrared)
	// Channel B: 4 (thermal infrared)

	// failure
	// Satellite: NOAA
	// Status: signal processing............................
	// wxtoimg: warning: couldn't find telemetry data
	// Gain: 12.6
	// Channel A: 3/3B (mid infrared)
	// Channel B: 4 (thermal infrared)
	private static boolean convert(DecoderResult observation, List<String> lines) {
		boolean success = true;
		for (String cur : lines) {
			int index = cur.indexOf(':');
			if (index == -1) {
				continue;
			}
			String name = cur.substring(0, index).trim();
			String value = cur.substring(index + 1).trim();
			if (name.equalsIgnoreCase("wxtoimg") && (value.equalsIgnoreCase("warning: couldn't find telemetry data") || value.contains("purchase upgrade key") || value.startsWith("error:"))) {
				success = false;
				continue;
			}
			if (name.equalsIgnoreCase("gain")) {
				observation.setGain(value);
				continue;
			}
			if (name.equalsIgnoreCase("Channel A")) {
				observation.setChannelA(value);
				continue;
			}
			if (name.equalsIgnoreCase("Channel B")) {
				observation.setChannelB(value);
				continue;
			}
		}
		return success;
	}

}

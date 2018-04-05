package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.APTResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class APTDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(APTDecoder.class);

	private final ProcessFactory factory;
	private final Configuration config;

	public APTDecoder(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	public APTResult decode(final File wavFile) {
		File image;
		try {
			image = File.createTempFile("apt", "a");
		} catch (IOException e1) {
			LOG.error("unable to create temp file", e1);
			return new APTResult();
		}
		ProcessWrapper process = null;
		try {
			process = factory.create(config.getProperty("satellites.wxtoimg.path") + " -e HVC -t n -c -o " + wavFile.getAbsolutePath() + " " + image.getAbsolutePath(), true, false);
			final InputStream is = process.getInputStream();
			final List<String> output = new ArrayList<>();
			Thread tis = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
						String curLine = null;
						while ((curLine = r.readLine()) != null) {
							synchronized (output) {
								LOG.info(curLine);
								output.add(curLine);
							}
						}
						r.close();
					} catch (Exception e) {
						LOG.error("unable to read input: " + wavFile.getAbsolutePath(), e);
					}
				}
			}, "wxtoimg-daemon");
			tis.setDaemon(true);
			tis.start();
			process.waitFor();
			return convert(image, output);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		}
		return new APTResult();
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
	private static APTResult convert(File image, List<String> lines) {
		APTResult result = new APTResult();
		boolean success = true;
		for (String cur : lines) {
			int index = cur.indexOf(":");
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
				result.setGain(value);
				continue;
			}
			if (name.equalsIgnoreCase("Channel A")) {
				result.setChannelA(value);
				continue;
			}
			if (name.equalsIgnoreCase("Channel B")) {
				result.setChannelB(value);
				continue;
			}
		}
		if (success) {
			result.setImage(image);
		}
		return result;
	}

}

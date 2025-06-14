package ru.r2cloud.satellite.decoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class APTDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(APTDecoder.class);

	private final ProcessFactory factory;
	private final Configuration config;
	private final ThreadPoolFactory threadFactory;

	public APTDecoder(Configuration config, ProcessFactory factory, ThreadPoolFactory threadFactory) {
		this.config = config;
		this.factory = factory;
		this.threadFactory = threadFactory;
	}

	@Override
	public DecoderResult decode(final File wavFile, final Observation request, final Transmitter transmitter, final Satellite satellite) {
		DecoderResult result = new DecoderResult();
		result.setIq(wavFile);
		Instrument avhrr3 = satellite.findById("AVHRR3");
		if (avhrr3 == null) {
			return result;
		}
		File combined = decodeCombinedImage(wavFile);
		if (combined == null) {
			return result;
		}
		Instrument copy = new Instrument(avhrr3);
		copy.setCombinedImage(combined);
		List<InstrumentChannel> channels = new ArrayList<>();
		InstrumentChannel channelA = decodeChannel(wavFile, avhrr3.getChannels(), "Channel A", "a");
		if (channelA != null) {
			channels.add(channelA);
		}
		InstrumentChannel channelB = decodeChannel(wavFile, avhrr3.getChannels(), "Channel B", "b");
		if (channelB != null) {
			channels.add(channelB);
		}
		copy.setChannels(channels);
		result.setInstruments(Collections.singletonList(copy));
		return result;
	}

	private InstrumentChannel decodeChannel(File wavFile, List<InstrumentChannel> channels, String channelName, String channelCommandLine) {
		File imageFile = new File(config.getTempDirectory(), "channel-" + channelCommandLine + ".jpg");
		ProcessWrapper process = null;
		try {
			process = factory.create(config.getProperty("satellites.wxtoimg.path") + " -" + channelCommandLine + " -t n -c -o " + wavFile.getAbsolutePath() + " " + imageFile.getAbsolutePath(), true, false);
			final InputStream is = process.getInputStream();
			ScheduledExecutorService executor = threadFactory.newScheduledThreadPool(1, new NamingThreadFactory("wxtoimg-daemon"));
			final List<String> lines = new ArrayList<>();
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
						String curLine = null;
						while ((curLine = r.readLine()) != null) {
							LOG.info(curLine);
							lines.add(curLine);
						}
						r.close();
					} catch (Exception e) {
						LOG.error("unable to read input: {}", wavFile.getAbsolutePath(), e);
					}
				}
			});
			process.waitFor();
			Util.shutdown(executor, config.getThreadPoolShutdownMillis());
			if (!isValidImage(lines)) {
				Util.deleteQuietly(imageFile);
				return null;
			}
			InstrumentChannel channel = findById(channels, convertToChannelId(getChannel(lines, channelName)));
			if (channel == null) {
				return null;
			}
			InstrumentChannel result = new InstrumentChannel(channel);
			result.setImage(imageFile);
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
			return null;
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to run", e);
			return null;
		}
	}

	private File decodeCombinedImage(File wavFile) {
		File image = new File(config.getTempDirectory(), "composite.jpg");
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
							LOG.info(curLine);
							lines.add(curLine);
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
			if (isValidImage(lines)) {
				return image;
			}
			Util.deleteQuietly(image);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
			return null;
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to run", e);
			return null;
		}
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
	private static boolean isValidImage(List<String> lines) {
		for (String cur : lines) {
			int index = cur.indexOf(':');
			if (index == -1) {
				continue;
			}
			String name = cur.substring(0, index).trim();
			String value = cur.substring(index + 1).trim();
			if (name.equalsIgnoreCase("wxtoimg") && (value.equalsIgnoreCase("warning: couldn't find telemetry data") || value.contains("purchase upgrade key") || value.startsWith("error:"))) {
				return false;
			}
		}
		return true;
	}

	private static String getChannel(List<String> lines, String expected) {
		for (String cur : lines) {
			int index = cur.indexOf(':');
			if (index == -1) {
				continue;
			}
			String name = cur.substring(0, index).trim();
			if (!name.equalsIgnoreCase(expected)) {
				continue;
			}
			return cur.substring(index + 1).trim();
		}
		return null;
	}

	private static String convertToChannelId(String wxtoimgChannel) {
		if (wxtoimgChannel == null) {
			return null;
		}
		if (wxtoimgChannel.startsWith("1")) {
			return "1";
		}
		if (wxtoimgChannel.startsWith("2")) {
			return "2";
		}
		if (wxtoimgChannel.startsWith("3/3A")) {
			return "3a";
		}
		if (wxtoimgChannel.startsWith("3/3B")) {
			return "3b";
		}
		if (wxtoimgChannel.startsWith("4")) {
			return "4";
		}
		if (wxtoimgChannel.startsWith("5")) {
			return "5";
		}
		return null;
	}

	private static InstrumentChannel findById(List<InstrumentChannel> channels, String id) {
		if (id == null) {
			return null;
		}
		for (InstrumentChannel cur : channels) {
			if (cur.getId().equalsIgnoreCase(id)) {
				return cur;
			}
		}
		return null;
	}

}

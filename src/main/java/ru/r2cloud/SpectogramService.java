package ru.r2cloud;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.sink.Spectogram;
import ru.r2cloud.jradio.source.InputStreamSource;
import ru.r2cloud.jradio.source.PlutoSdr;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);
	private static final int OPTIMAL_WIDTH = 1024;

	private final ProcessFactory factory;
	private final Configuration config;

	public SpectogramService(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	public File create(Observation observation) {
		if (observation == null) {
			return null;
		}
		if (observation.getRawPath() == null || !observation.getRawPath().exists() || observation.getRawPath().length() == 0) {
			LOG.info("[{}] iq file is missing. skip spectogram", observation.getId());
			return null;
		}
		LOG.info("[{}] generating spectogram", observation.getId());
		File result;
		if (observation.getRawPath().getName().endsWith(".wav")) {
			result = createFromWav(observation);
		} else {
			result = createFromIq(observation);
		}
		if (result != null) {
			LOG.info("[{}] spectogram created", observation.getId());
		}
		return result;
	}

	private File createFromWav(Observation req) {
		File file = req.getRawPath();
		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			WavFileSource source = new WavFileSource(is);
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + req.getId() + ".png");
			ImageIO.write(image, "png", tmp);
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

	private File createFromIq(Observation req) {
		// if sdr_spectrogram available then use it
		// it is much faster
		if (config.getBoolean("satellites.sdrspectrogram.available")) {
			return createFromIqSdrSpectrogram(req);
		}
		return createFromIqJradio(req);
	}

	private File createFromIqSdrSpectrogram(Observation req) {
		ProcessWrapper process = null;
		File tmp = new File(config.getTempDirectory(), "spectogram-" + req.getId() + ".png");
		try {
			process = factory.create(config.getProperty("satellites.sdrspectrogram.path") + " -w " + OPTIMAL_WIDTH + " -s " + req.getSampleRate() + " -d " + req.getDataFormat().getExtension() + " -i " + req.getRawPath().getAbsolutePath() + " -o " + tmp.getAbsolutePath(), true, false);
			int code = process.waitFor();
			if (code != 0) {
				LOG.error("unable to create spectrogram using sdr_spectrogram: {}", code);
				tmp.delete(); // ignore status code because the file might not exist
				return null;
			}
			return tmp;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (IOException e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

	private File createFromIqJradio(Observation req) {
		Long totalBytes = Util.readTotalBytes(req.getRawPath().toPath());
		if (totalBytes == null) {
			return null;
		}
		if (totalBytes < 0) {
			LOG.error("corrupted raw file: {}", req.getRawPath().getAbsolutePath());
			return null;
		}
		if (req.getDataFormat() == null) {
			LOG.error("data format is missing");
			return null;
		}
		FloatInput source = null;
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(req.getRawPath()));
			if (req.getRawPath().toString().endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			switch (req.getDataFormat()) {
			case COMPLEX_UNSIGNED_BYTE:
				source = new RtlSdr(is, req.getSampleRate(), totalBytes / 2);
				break;
			case COMPLEX_SIGNED_SHORT:
				source = new PlutoSdr(is, req.getSampleRate(), totalBytes / 4);
				break;
			case COMPLEX_FLOAT:
				Context ctx = new Context();
				ctx.setChannels(2);
				ctx.setSampleSizeInBits(4 * 8); // float = 4 bytes
				ctx.setSampleRate(req.getSampleRate());
				ctx.setTotalSamples(totalBytes / 8);
				source = new InputStreamSource(is, ctx);
				break;
			default:
				throw new IllegalArgumentException("unsupported data format: " + req.getDataFormat());
			}
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + req.getId() + ".png");
			ImageIO.write(image, "png", tmp);
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		} finally {
			Util.closeQuietly(source);
		}
	}

}

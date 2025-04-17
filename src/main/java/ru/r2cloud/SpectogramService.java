package ru.r2cloud;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import ru.r2cloud.util.Util;
import ru.r2cloud.util.ZiqInputStream;

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);
	private static final int OPTIMAL_WIDTH = 1024;

	private final Configuration config;

	public SpectogramService(Configuration config) {
		this.config = config;
	}

	public File create(Observation observation) {
		if (observation == null) {
			return null;
		}
		if (observation.getRawPath() == null || !observation.getRawPath().exists() || observation.getRawPath().length() == 0) {
			return null;
		}
		LOG.info("[{}] generating spectogram", observation.getId());
		File result;
		if (observation.getRawPath().getName().endsWith(".wav")) {
			result = createFromWav(observation.getRawPath());
		} else {
			result = createFromIq(observation);
		}
		if (result != null) {
			LOG.info("[{}] spectogram created", observation.getId());
		}
		return result;
	}

	private File createFromWav(File file) {
		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			WavFileSource source = new WavFileSource(is);
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + file.getName() + ".png");
			ImageIO.write(image, "png", tmp);
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

	private File createFromIq(Observation req) {
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
			case ZIQ:
				source = new ZiqInputStream(is, req.getSampleRate(), (long) (((double) req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000 * req.getSampleRate())); // very rough estimation
				break;
			default:
				throw new IllegalArgumentException("unsupported data format: " + req.getDataFormat());
			}
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + req.getRawPath().getName() + ".png");
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

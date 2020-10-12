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

import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.sink.Spectogram;
import ru.r2cloud.jradio.source.PlutoSdr;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);
	private static final int OPTIMAL_WIDTH = 1024;

	private final Configuration config;

	public SpectogramService(Configuration config) {
		this.config = config;
	}

	public File create(Observation observation) {
		LOG.info("generating spectogram");
		if (observation == null) {
			return null;
		}
		if (observation.getRawPath() == null) {
			return null;
		}
		if (observation.getRawPath().getName().endsWith(".wav")) {
			return createFromWav(observation.getRawPath());
		} else {
			return createFromIq(observation);
		}
	}

	private File createFromWav(File file) {
		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			WavFileSource source = new WavFileSource(is);
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + file.getName() + ".png");
			ImageIO.write(image, "png", tmp);
			LOG.info("spectogram created");
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}
	
	private File createFromIq(Observation req) {
		Long totalSamples = Util.readTotalSamples(req.getRawPath().toPath());
		if (totalSamples == null) {
			return null;
		}
		if (totalSamples < 0) {
			LOG.error("corrupted raw file: {}", req.getRawPath().getAbsolutePath());
			return null;
		}
		FloatInput source = null;
		try {
			switch (req.getSdrType()) {
			case RTLSDR:
				source = new RtlSdr(new GZIPInputStream(new FileInputStream(req.getRawPath())), req.getInputSampleRate(), totalSamples);
				break;
			case PLUTOSDR:
				source = new PlutoSdr(new GZIPInputStream(new FileInputStream(req.getRawPath())), req.getInputSampleRate(), totalSamples / 2);
				break;
			default:
				throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
			}
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + req.getRawPath().getName() + ".png");
			ImageIO.write(image, "png", tmp);
			LOG.info("spectogram created");
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		} finally {
			Util.closeQuietly(source);
		}
	}

}

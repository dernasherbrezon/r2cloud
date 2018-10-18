package ru.r2cloud;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.sink.Spectogram;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.util.Configuration;

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);
	private static final int OPTIMAL_WIDTH = 1024;

	private final Configuration config;

	public SpectogramService(Configuration config) {
		this.config = config;
	}

	public File create(File wavFile) {
		LOG.info("generating spectogram");
		try (InputStream is = new BufferedInputStream(new FileInputStream(wavFile))) {
			WavFileSource source = new WavFileSource(is);
			Spectogram spectogram = new Spectogram((int) (source.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(source);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + wavFile.getName() + ".png");
			ImageIO.write(image, "png", tmp);
			LOG.info("spectogram created");
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

}

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

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);

	private final Spectogram spectogram = new Spectogram(2, 1024);

	public File create(File wavFile) {
		LOG.info("generating spectogram");
		try (InputStream is = new BufferedInputStream(new FileInputStream(wavFile))) {
			WavFileSource source = new WavFileSource(is);
			BufferedImage image = spectogram.process(source);
			File tmp = File.createTempFile("spectogram", ".png");
			ImageIO.write(image, "png", tmp);
			LOG.info("spectogram created");
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

}

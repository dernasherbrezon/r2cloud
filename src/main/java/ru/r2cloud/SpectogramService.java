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

import ru.r2cloud.jradio.sink.Spectogram;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SpectogramService {

	private static final Logger LOG = LoggerFactory.getLogger(SpectogramService.class);
	private static final int OPTIMAL_WIDTH = 1024;

	private final Configuration config;

	public SpectogramService(Configuration config) {
		this.config = config;
	}

	public File create(ObservationFull observation) {
		LOG.info("generating spectogram");
		if (observation == null || observation.getResult() == null) {
			return null;
		}
		if (observation.getResult().getWavPath() != null) {
			return createFromWav(observation.getResult().getWavPath());
		} else if (observation.getResult().getIqPath() != null) {
			if (observation.getReq() == null) {
				return null;
			}
			return createFromIq(observation.getResult().getIqPath(), observation.getReq().getInputSampleRate());
		}
		return null;
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

	private File createFromIq(File file, float sampleRate) {
		Long totalSamples = Util.readTotalSamples(file);
		if (totalSamples == null) {
			return null;
		}
		if (totalSamples < 0) {
			LOG.error("corrupted raw file: {}", file.getAbsolutePath());
			return null;
		}
		try (RtlSdr sdr = new RtlSdr(new GZIPInputStream(new FileInputStream(file)), sampleRate, totalSamples)) {
			Spectogram spectogram = new Spectogram((int) (sdr.getContext().getSampleRate() / OPTIMAL_WIDTH));
			BufferedImage image = spectogram.process(sdr);
			File tmp = new File(config.getTempDirectory(), "spectogram-" + file.getName() + ".png");
			ImageIO.write(image, "png", tmp);
			LOG.info("spectogram created");
			return tmp;
		} catch (Exception e) {
			LOG.error("unable to create spectogram", e);
			return null;
		}
	}

}

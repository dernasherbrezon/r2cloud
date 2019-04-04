package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.sink.WavFileSink;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class RtlSdrReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(RtlSdrReader.class);

	private ProcessWrapper rtlSdr = null;

	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;

	public RtlSdrReader(Configuration config, ProcessFactory factory, ObservationRequest req) {
		this.config = config;
		this.factory = factory;
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw");
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			startTimeMillis = System.currentTimeMillis();
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g 45 -p " + ppm + " " + rawFile.getAbsolutePath(), Redirect.INHERIT, true);
			int responseCode = rtlSdr.waitFor();
			LOG.info("[{}] rtl_sdr stopped: {}", req.getId(), responseCode);
		} catch (IOException e) {
			LOG.error("[" + req.getId() + "] unable to run", e);
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);

		if (rawFile.exists()) {
			LOG.info("[{}] decimating the data", req.getId());
			File wavPath = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".wav");
			WavFileSink sink = null;
			FileOutputStream fos = null;
			try {
				RtlSdr sdr = new RtlSdr(rawFile, req.getInputSampleRate());
				float[] taps = Firdes.lowPass(1.0, sdr.getContext().getSampleRate(), sdr.getContext().getSampleRate() / 2, 600, Window.WIN_HAMMING, 6.76);
				FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(sdr, taps, req.getInputSampleRate() / req.getOutputSampleRate(), req.getSatelliteFrequency() - req.getActualFrequency());
				sink = new WavFileSink(xlating);
				fos = new FileOutputStream(wavPath);
				sink.process(fos);
				LOG.info("[{}] decimation completed. from: {} to {}", req.getId(), rawFile.getAbsolutePath(), wavPath.getAbsolutePath());
				result.setWavFile(wavPath);
			} catch (IOException e) {
				LOG.error("[" + req.getId() + "] unable to run", e);
				if (wavPath.exists() && !wavPath.delete()) {
					LOG.error("[{}] unable to delete wav file at: {}", req.getId(), wavPath.getAbsolutePath());
				}
			} finally {
				if (!rawFile.delete()) {
					LOG.error("[{}] unable to delete raw file at: {}", req.getId(), rawFile.getAbsolutePath());
				}
				Util.closeQuietly(sink);
				Util.closeQuietly(fos);
			}
		}
		return result;
	}

	@Override
	public void complete() {
		Util.shutdown("rtl_sdr for " + req.getId(), rtlSdr, 10000);
		rtlSdr = null;
	}

}

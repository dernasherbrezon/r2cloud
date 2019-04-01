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
	private File rawFile;
	private Long startTimeMillis = null;
	private Long endTimeMillis = null;

	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;

	public RtlSdrReader(Configuration config, ProcessFactory factory, ObservationRequest req) {
		this.config = config;
		this.factory = factory;
		this.req = req;
	}

	@Override
	public void start() {
		this.rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw");
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			if (startTimeMillis == null) {
				startTimeMillis = System.currentTimeMillis();
			}
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g 45 -p " + ppm + " " + rawFile.getAbsolutePath(), Redirect.INHERIT, true);
			int responseCode = rtlSdr.waitFor();
			LOG.info("[{}] rtl_sdr stopped: {}", req.getId(), responseCode);
		} catch (IOException e) {
			LOG.error("[" + req.getId() + "] unable to run", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
	}

	@Override
	public IQData complete() {
		Util.shutdown("rtl_sdr for " + req.getId(), rtlSdr, 10000);
		rtlSdr = null;

		IQData result = new IQData();

		if (rawFile != null && rawFile.exists()) {
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
			} catch (Exception e) {
				LOG.error("unable to run", e);
			} finally {
				if (!rawFile.delete()) {
					LOG.error("[{}] unable to delete raw file at: {}", req.getId(), rawFile.getAbsolutePath());
				}
				Util.closeQuietly(sink);
				Util.closeQuietly(fos);
			}
		}
		if (startTimeMillis != null) {
			result.setActualStart(startTimeMillis);
		} else {
			// just to be on the safe side
			result.setActualStart(req.getStartTimeMillis());
		}
		if (endTimeMillis != null) {
			result.setActualEnd(endTimeMillis);
		} else {
			result.setActualEnd(req.getEndTimeMillis());
		}

		return result;
	}

}

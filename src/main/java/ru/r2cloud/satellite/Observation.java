package ru.r2cloud.satellite;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.Util;

public class Observation {

	private static final Logger LOG = LoggerFactory.getLogger(Observation.class);
	private static final int BUF_SIZE = 0x1000; // 4K

	private ProcessWrapper rtlfm = null;

	private final File outputDirectory;
	private final File wavPath;
	private final Satellite satellite;
	private final Configuration config;
	private final SatPass nextPass;
	private final ProcessFactory factory;

	public Observation(Configuration config, Satellite satellite, SatPass nextPass, ProcessFactory factory) {
		this.config = config;
		this.satellite = satellite;
		this.nextPass = nextPass;
		this.outputDirectory = new File(Util.initDirectory(config.getProperty("satellites.basepath.location")), satellite.getId() + File.separator + "data" + File.separator + nextPass.getStart().getTime().getTime());
		this.wavPath = new File(outputDirectory, "output.wav");
		this.factory = factory;
	}

	public void start() {
		if (!outputDirectory.mkdirs()) {
			LOG.info("unable to create output directory: " + outputDirectory.getAbsolutePath());
			return;
		}
		ProcessWrapper sox = null;
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r 60000 -es -b 16 - " + wavPath.getAbsolutePath() + " rate 11025", Redirect.INHERIT, false);
			rtlfm = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + String.valueOf(satellite.getFrequency()) + " -s 60k -g 45 -p " + String.valueOf(ppm) + " -E deemp -F 9 -", Redirect.INHERIT, false);
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = rtlfm.getInputStream().read(buf);
				if (r == -1) {
					break;
				}
				sox.getOutputStream().write(buf, 0, r);
			}
			sox.getOutputStream().flush();
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} finally {
			LOG.info("stopping pipe thread");
			Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
			Util.shutdown("sox", sox, 10000);
		}
	}

	public void stop() {
		Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
		rtlfm = null;

		if (!wavPath.exists()) {
			LOG.info("nothing saved. cleanup current directory: " + outputDirectory.getAbsolutePath());
			Util.deleteDirectory(outputDirectory);
			return;
		}

		processSource(wavPath, "a");
		processSource(wavPath, "b");

		if (!wavPath.delete()) {
			LOG.error("unable to delete source .wav: " + wavPath.getAbsolutePath());
		}

		File[] dataDirs = outputDirectory.getParentFile().listFiles();
		Integer maxCount = config.getInteger("scheduler.data.retention.count");
		if (dataDirs.length > maxCount) {
			Arrays.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
			for (int i = 0; i < (dataDirs.length - maxCount); i++) {
				Util.deleteDirectory(dataDirs[i]);
			}
		}
	}

	private void processSource(final File wavFile, String type) {
		File result = new File(wavFile.getParentFile(), type + ".jpg");
		ProcessWrapper process = null;
		try {
			process = factory.create(config.getProperty("satellites.wxtoimg.path") + " -t n -" + type + " -c -o " + wavFile.getAbsolutePath() + " " + result.getAbsolutePath(), null, true);
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("wxtoimg", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		}
	}

	public SatPass getNextPass() {
		return nextPass;
	}

}

package ru.r2cloud.satellite;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.aausat4.AAUSAT4;
import ru.r2cloud.jradio.blocks.BinarySlicer;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class Aausat4Observation implements Observation {

	private static final Logger LOG = LoggerFactory.getLogger(Aausat4Observation.class);
	private static final int BUF_SIZE = 0x1000; // 4K

	private ProcessWrapper rtlfm = null;
	private File wavPath;

	private final Satellite satellite;
	private final Configuration config;
	private final SatPass nextPass;
	private final ProcessFactory factory;
	private final ObservationResultDao dao;
	private final String observationId;

	public Aausat4Observation(Configuration config, Satellite satellite, SatPass nextPass, ProcessFactory factory, ObservationResultDao dao) {
		this.config = config;
		this.satellite = satellite;
		this.nextPass = nextPass;
		this.factory = factory;
		this.dao = dao;
		this.observationId = String.valueOf(nextPass.getStart().getTime().getTime());
	}

	@Override
	public void start() {
		try {
			this.wavPath = File.createTempFile(satellite.getId() + "-", ".wav");
		} catch (IOException e) {
			LOG.error("unable to create temp file", e);
			return;
		}
		ProcessWrapper sox = null;
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r 60000 -es -b 16 - " + wavPath.getAbsolutePath() + " rate 11025", Redirect.INHERIT, false);
			rtlfm = factory.create(config.getProperty("satellites.rtlfm.path") + " -f " + String.valueOf(satellite.getFrequency()) + " -s 60k -g 45 -p " + String.valueOf(ppm) + " -E deemp -F 9 -", Redirect.INHERIT, false);
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
			Util.shutdown("rtl_fm for satellites", rtlfm, 10000);
			Util.shutdown("sox", sox, 10000);
		}
	}

	@Override
	public void stop() {
		Util.shutdown("rtlfm for satellites", rtlfm, 10000);
		rtlfm = null;

		if (!wavPath.exists()) {
			LOG.info("nothing saved");
			return;
		}

		if (!dao.createObservation(satellite.getId(), observationId, wavPath)) {
			return;
		}
	}

	@Override
	public void decode() {
		ObservationResult cur = dao.find(satellite.getId(), observationId);
		if (cur == null) {
			return;
		}
		Context context = new Context();
		File file;
		try {
			file = File.createTempFile(satellite.getId() + "-", ".bin");
		} catch (IOException e) {
			LOG.error("unable to create temp file", e);
			return;
		}
		long numberOfDecodedPackets = 0;
		try (FileOutputStream fos = new FileOutputStream(file);
				AAUSAT4 input = new AAUSAT4(new TaggedStreamToPdu(context, new UnpackedToPacked(context, new FixedLengthTagger(context, new CorrelateAccessCodeTag(context, new BinarySlicer(new ClockRecoveryMM(new WavFileSource(new BufferedInputStream(new FileInputStream(cur.getWavPath()))), 20.0f, (float) (0.25 * 0.175 * 0.175), 0.005f, 0.175f, 0.005f)), 8, "010011110101101000110100010000110101010101000010"), 2008), 1, Endianness.GR_MSB_FIRST, Byte.class)));) {
			while (input.hasNext()) {
				fos.write(input.next().getData());
				numberOfDecodedPackets++;
			}
		} catch (Exception e) {
			LOG.error("unable to decode", e);
			return;
		}
		dao.saveData(satellite.getId(), observationId, file);

		cur.setStart(nextPass.getStart().getTime());
		cur.setEnd(nextPass.getEnd().getTime());
		cur.setNumberOfDecodedPackets(numberOfDecodedPackets);
		dao.saveMeta(satellite.getId(), cur);
	}

	@Override
	public Date getStart() {
		return nextPass.getStart().getTime();
	}

	@Override
	public Date getEnd() {
		return nextPass.getEnd().getTime();
	}

	@Override
	public String getId() {
		return observationId;
	}

}

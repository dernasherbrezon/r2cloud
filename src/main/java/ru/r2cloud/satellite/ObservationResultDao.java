package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class ObservationResultDao {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationResultDao.class);

	private final File basepath;
	private final Integer maxCount;

	public ObservationResultDao(Configuration config) {
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.maxCount = config.getInteger("scheduler.data.retention.count");
	}

	public List<ObservationResult> findAllBySatelliteId(String satelliteId) {
		File dataRoot = new File(basepath, satelliteId + File.separator + "data");
		if (!dataRoot.exists()) {
			return Collections.emptyList();
		}
		File[] observations = dataRoot.listFiles();
		Arrays.sort(observations, FilenameComparator.INSTANCE_DESC);
		List<ObservationResult> result = new ArrayList<ObservationResult>(observations.length);
		for (File curDirectory : observations) {
			ObservationResult cur = find(satelliteId, curDirectory);
			if (cur == null) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	public ObservationResult find(String satelliteId, String observationId) {
		File baseDirectory = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId);
		if (!baseDirectory.exists()) {
			return null;
		}
		return find(satelliteId, baseDirectory);
	}

	private static ObservationResult find(String satelliteId, File curDirectory) {
		long startTime;
		try {
			startTime = Long.valueOf(curDirectory.getName());
		} catch (Exception e) {
			return null;
		}
		ObservationResult cur = new ObservationResult();
		cur.setSatelliteId(satelliteId);
		cur.setId(curDirectory.getName());
		cur.setStart(new Date(startTime));
		File a = new File(curDirectory, "a.jpg");
		if (a.exists()) {
			cur.setaPath(a);
			cur.setaURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/a.jpg");
		}
		File data = new File(curDirectory, "data.bin");
		if (data.exists()) {
			cur.setDataPath(data);
			cur.setDataURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/data.bin");
		}
		File wav = new File(curDirectory, "output.wav");
		if (wav.exists()) {
			cur.setWavPath(wav);
		}
		File spectogram = new File(curDirectory, "spectogram.png");
		if (spectogram.exists()) {
			cur.setSpectogramPath(spectogram);
			cur.setSpectogramURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/spectogram.png");
		}
		File dest = new File(curDirectory, "meta.json");
		if (dest.exists()) {
			try (BufferedReader r = new BufferedReader(new FileReader(dest))) {
				JsonObject meta = Json.parse(r).asObject();
				cur.setEnd(new Date(meta.getLong("end", -1L)));
				cur.setGain(meta.getString("gain", null));
				cur.setChannelA(meta.getString("channelA", null));
				cur.setChannelB(meta.getString("channelB", null));
				cur.setSampleRate(meta.getInt("sampleRate", -1));
				cur.setFrequency(meta.getLong("frequency", -1L));
				JsonValue numberOfPacketsDecodedStr = meta.get("numberOfDecodedPackets");
				if (numberOfPacketsDecodedStr != null) {
					cur.setNumberOfDecodedPackets(numberOfPacketsDecodedStr.asLong());
				}
			} catch (Exception e) {
				LOG.error("unable to load meta", e);
			}
		}
		return cur;
	}

	public boolean saveChannel(String satelliteId, String observationId, File a, String type) {
		File dest = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId + File.separator + type + ".jpg");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean saveData(String satelliteId, String observationId, File a) {
		File dest = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId + File.separator + "data.bin");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean saveSpectogram(String satelliteId, String observationId, File a) {
		File dest = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId + File.separator + "spectogram.png");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean createObservation(String satelliteId, String observationId, File wavPath) {
		File[] dataDirs = new File(basepath, satelliteId + File.separator + "data").listFiles();
		if (dataDirs != null && dataDirs.length > maxCount) {
			Arrays.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
			for (int i = 0; i < (dataDirs.length - maxCount); i++) {
				Util.deleteDirectory(dataDirs[i]);
			}
		}

		File dest = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId + File.separator + "output.wav");
		if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
			LOG.info("unable to create parent dir:" + dest.getParentFile().getAbsolutePath());
			return false;
		}
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return wavPath.renameTo(dest);
	}

	public void saveMeta(String id, ObservationResult cur) {
		JsonObject meta = new JsonObject();
		meta.add("start", cur.getStart().getTime());
		meta.add("end", cur.getEnd().getTime());
		meta.add("sampleRate", cur.getSampleRate());
		meta.add("frequency", cur.getFrequency());
		if (cur.getGain() != null) {
			meta.add("gain", cur.getGain());
		}
		if (cur.getChannelA() != null) {
			meta.add("channelA", cur.getChannelA());
		}
		if (cur.getChannelB() != null) {
			meta.add("channelB", cur.getChannelB());
		}
		if (cur.getNumberOfDecodedPackets() != null) {
			meta.add("numberOfDecodedPackets", cur.getNumberOfDecodedPackets());
		}
		File dest = new File(basepath, id + File.separator + "data" + File.separator + cur.getId() + File.separator + "meta.json");
		try (BufferedWriter w = new BufferedWriter(new FileWriter(dest))) {
			w.append(meta.toString());
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
		}
	}
}

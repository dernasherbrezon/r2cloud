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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
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

	public List<ObservationFull> findAllBySatelliteId(String satelliteId) {
		File dataRoot = new File(basepath, satelliteId + File.separator + "data");
		if (!dataRoot.exists()) {
			return Collections.emptyList();
		}
		File[] observations = dataRoot.listFiles();
		Arrays.sort(observations, FilenameComparator.INSTANCE_DESC);
		List<ObservationFull> result = new ArrayList<ObservationFull>(observations.length);
		for (File curDirectory : observations) {
			ObservationFull cur = find(satelliteId, curDirectory);
			// some directories might be corrupted
			if (cur == null) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	public ObservationFull find(String satelliteId, String observationId) {
		File baseDirectory = new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId);
		if (!baseDirectory.exists()) {
			return null;
		}
		return find(satelliteId, baseDirectory);
	}

	private static ObservationFull find(String satelliteId, File curDirectory) {
		File dest = new File(curDirectory, "meta.json");
		if (!dest.exists()) {
			return null;
		}
		ObservationFull full;
		try (BufferedReader r = new BufferedReader(new FileReader(dest))) {
			JsonObject meta = Json.parse(r).asObject();
			full = ObservationFull.fromJson(meta);
		} catch (Exception e) {
			LOG.error("unable to load meta", e);
			return null;
		}

		ObservationResult result = full.getResult();

		File a = new File(curDirectory, "a.jpg");
		if (a.exists()) {
			result.setaPath(a);
			result.setaURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getReq().getId() + "/a.jpg");
		}
		File data = new File(curDirectory, "data.bin");
		if (data.exists()) {
			result.setDataPath(data);
			result.setDataURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getReq().getId() + "/data.bin");
		}
		File wav = new File(curDirectory, "output.wav");
		if (wav.exists()) {
			result.setWavPath(wav);
		}
		File spectogram = new File(curDirectory, "spectogram.png");
		if (spectogram.exists()) {
			result.setSpectogramPath(spectogram);
			result.setSpectogramURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getReq().getId() + "/spectogram.png");
		}

		return full;
	}

	public File saveImage(String satelliteId, String observationId, File a) {
		File dest = new File(getObservationBasepath(satelliteId, observationId), "a.jpg");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: {}", dest.getAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest)) {
			return null;
		}
		return dest;
	}

	public File saveData(String satelliteId, String observationId, File a) {
		File dest = new File(getObservationBasepath(satelliteId, observationId), "data.bin");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: {}", dest.getAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest)) {
			return null;
		}
		return dest;
	}

	public boolean saveSpectogram(String satelliteId, String observationId, File a) {
		File dest = new File(getObservationBasepath(satelliteId, observationId), "spectogram.png");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: {}", dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public File insert(ObservationRequest observation, File wavPath) {
		File[] dataDirs = new File(basepath, observation.getSatelliteId() + File.separator + "data").listFiles();
		if (dataDirs != null && dataDirs.length > maxCount) {
			Arrays.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
			for (int i = 0; i < (dataDirs.length - maxCount); i++) {
				Util.deleteDirectory(dataDirs[i]);
			}
		}

		File observationBasePath = getObservationBasepath(observation);
		if (!observationBasePath.exists() && !observationBasePath.mkdirs()) {
			LOG.info("unable to create parent dir: {}", observationBasePath.getAbsolutePath());
			return null;
		}

		ObservationFull full = new ObservationFull(observation);
		if (!update(full)) {
			return null;
		}

		return insertIQData(observation, wavPath);
	}

	private File insertIQData(ObservationRequest observation, File wavPath) {
		File dest = new File(getObservationBasepath(observation), "output.wav");
		if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
			LOG.info("unable to create parent dir: {}", dest.getParentFile().getAbsolutePath());
			return null;
		}
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: {}", dest.getAbsolutePath());
			return null;
		}
		if (!wavPath.renameTo(dest)) {
			return null;
		}
		return dest;
	}

	public boolean update(ObservationFull cur) {
		JsonObject meta = cur.toJson();
		File dest = new File(getObservationBasepath(cur.getReq()), "meta.json");
		try (BufferedWriter w = new BufferedWriter(new FileWriter(dest))) {
			w.append(meta.toString());
			return true;
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
			return false;
		}
	}

	private File getObservationBasepath(ObservationRequest observation) {
		return getObservationBasepath(observation.getSatelliteId(), observation.getId());
	}

	private File getObservationBasepath(String satelliteId, String observationId) {
		return new File(basepath, satelliteId + File.separator + "data" + File.separator + observationId);
	}
}

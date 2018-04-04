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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private final List<Satellite> satellites;
	private final File basepath;
	private final Integer maxCount;
	private final Map<String, Satellite> satelliteByName = new HashMap<String, Satellite>();

	public SatelliteDao(Configuration config) {
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.maxCount = config.getInteger("scheduler.data.retention.count");
		satellites = new ArrayList<Satellite>();
		for (String cur : Util.splitComma(config.getProperty("satellites.supported"))) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			curSatellite.setName(config.getProperty("satellites." + curSatellite.getId() + ".name"));
			curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
			curSatellite.setDecoder(config.getProperty("satellites." + curSatellite.getId() + ".decoder"));
			index(curSatellite);
		}
	}

	public Satellite findByName(String name) {
		return satelliteByName.get(name);
	}

	public List<Satellite> findSupported() {
		return satellites;
	}

	private void index(Satellite satellite) {
		satellites.add(satellite);
		satelliteByName.put(satellite.getName(), satellite);
	}

	public List<ObservationResult> findWeatherObservations(String id) {
		File dataRoot = new File(basepath, id + File.separator + "data");
		if (!dataRoot.exists()) {
			return Collections.emptyList();
		}
		File[] observations = dataRoot.listFiles();
		Arrays.sort(observations, FilenameComparator.INSTANCE_DESC);
		List<ObservationResult> result = new ArrayList<ObservationResult>(observations.length);
		for (File curDirectory : observations) {
			ObservationResult cur = load(id, curDirectory);
			result.add(cur);
		}
		return result;
	}

	public ObservationResult find(String satelliteId, String id) {
		File baseDirectory = new File(basepath, satelliteId + File.separator + "data" + File.separator + id);
		if (!baseDirectory.exists()) {
			return null;
		}
		return load(satelliteId, baseDirectory);
	}

	private static ObservationResult load(String satelliteId, File curDirectory) {
		ObservationResult cur = new ObservationResult();
		cur.setId(curDirectory.getName());
		cur.setStart(new Date(Long.valueOf(curDirectory.getName())));
		File a = new File(curDirectory, "a.jpg");
		if (a.exists()) {
			cur.setaURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/a.jpg");
		}
		File b = new File(curDirectory, "b.jpg");
		if (b.exists()) {
			cur.setbURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/b.jpg");
		}
		File wav = new File(curDirectory, "output.wav");
		if (wav.exists()) {
			cur.setWavPath(wav);
		}
		File spectogram = new File(curDirectory, "spectogram.png");
		if (spectogram.exists()) {
			cur.setSpectogramURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + cur.getId() + "/spectogram.png");
		}
		return cur;
	}

	public ObservationResult findMeta(String satelliteId, String id) {
		ObservationResult result = find(satelliteId, id);
		if (result == null) {
			return null;
		}
		File dest = new File(basepath, satelliteId + File.separator + "data" + File.separator + id + File.separator + "meta.json");
		if (!dest.exists()) {
			return result;
		}
		try (BufferedReader r = new BufferedReader(new FileReader(dest))) {
			JsonObject meta = Json.parse(r).asObject();
			result.setStart(new Date(meta.getLong("start", -1L)));
			result.setEnd(new Date(meta.getLong("end", -1L)));
			result.setGain(meta.getString("gain", null));
			result.setChannelA(meta.getString("channelA", null));
			result.setChannelB(meta.getString("channelB", null));
		} catch (Exception e) {
			LOG.error("unable to load meta", e);
		}
		return result;
	}

	public boolean saveChannel(String id, String observationId, File a, String type) {
		File dest = new File(basepath, id + File.separator + "data" + File.separator + observationId + File.separator + type + ".jpg");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean saveSpectogram(String id, String observationId, File a) {
		File dest = new File(basepath, id + File.separator + "data" + File.separator + observationId + File.separator + "spectogram.png");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean createObservation(String id, String observationId, File file) {
		File[] dataDirs = new File(basepath, id + File.separator + "data").listFiles();
		if (dataDirs != null && dataDirs.length > maxCount) {
			Arrays.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
			for (int i = 0; i < (dataDirs.length - maxCount); i++) {
				Util.deleteDirectory(dataDirs[i]);
			}
		}

		File dest = new File(basepath, id + File.separator + "data" + File.separator + observationId + File.separator + "output.wav");
		if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
			LOG.info("unable to create parent dir:" + dest.getParentFile().getAbsolutePath());
			return false;
		}
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return file.renameTo(dest);
	}

	public void saveMeta(String id, ObservationResult cur) {
		JsonObject meta = new JsonObject();
		meta.add("start", cur.getStart().getTime());
		meta.add("end", cur.getEnd().getTime());
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

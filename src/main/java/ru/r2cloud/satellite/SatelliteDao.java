package ru.r2cloud.satellite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private static final Pattern SATELLITE_ID = Pattern.compile("(\\d+)\\((.*?)\\)");
	private final List<Satellite> satellites;
	private final File basepath;
	private final Integer maxCount;
	private final Map<String, Satellite> satelliteByName = new HashMap<String, Satellite>();

	public SatelliteDao(Configuration config) {
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.maxCount = config.getInteger("scheduler.data.retention.count");
		satellites = new ArrayList<Satellite>();
		for (String cur : Util.splitComma(config.getProperty("satellites.supported"))) {
			Matcher m = SATELLITE_ID.matcher(cur);
			if (m.find()) {
				Satellite curSatellite = new Satellite();
				curSatellite.setName(m.group(2));
				curSatellite.setId(m.group(1));
				curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
				index(curSatellite);
			}
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

	private static ObservationResult load(String id, File curDirectory) {
		ObservationResult cur = new ObservationResult();
		cur.setId(curDirectory.getName());
		cur.setDate(new Date(Long.valueOf(curDirectory.getName())));
		File a = new File(curDirectory, "a.jpg");
		if (a.exists()) {
			cur.setaPath("/api/v1/admin/static/satellites/" + id + "/data/" + curDirectory.getName() + "/a.jpg");
		}
		File b = new File(curDirectory, "b.jpg");
		if (b.exists()) {
			cur.setbPath("/api/v1/admin/static/satellites/" + id + "/data/" + curDirectory.getName() + "/b.jpg");
		}
		File wav = new File(curDirectory, "output.wav");
		if (wav.exists()) {
			cur.setWavPath(wav);
		}
		return cur;
	}

	public boolean saveChannel(String id, String observationId, File a, String type) {
		File dest = new File(basepath, id + File.separator + "data" + File.separator + observationId + File.separator + type + ".jpg");
		if (dest.exists()) {
			LOG.info("unable to save. dest already exist: " + dest.getAbsolutePath());
			return false;
		}
		return a.renameTo(dest);
	}

	public boolean createObservation(String id, String observationId, File wavPath) {
		File[] dataDirs = new File(basepath, id + File.separator + "data").listFiles();
		if (dataDirs.length > maxCount) {
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
		return wavPath.renameTo(dest);
	}

	public void saveMeta(String id, String observationId, JsonObject meta) {
		File dest = new File(basepath, id + File.separator + "data" + File.separator + observationId + File.separator + "meta.json");
		try (BufferedWriter w = new BufferedWriter(new FileWriter(dest))) {
			w.append(meta.toString());
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
		}
	}

}

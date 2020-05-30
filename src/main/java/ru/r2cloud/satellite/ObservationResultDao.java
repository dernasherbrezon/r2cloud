package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class ObservationResultDao {

	private static final String DEST_ALREADY_EXIST_MESSAGE = "unable to save. dest already exist: {}";
	private static final String SPECTOGRAM_FILENAME = "spectogram.png";
	private static final String OUTPUT_WAV_FILENAME = "output.wav";
	private static final String OUTPUT_RAW_FILENAME = "output.raw.gz";

	private static final Logger LOG = LoggerFactory.getLogger(ObservationResultDao.class);

	private final Path basepath;
	private final Integer maxCount;

	public ObservationResultDao(Configuration config) {
		this.basepath = config.getSatellitesBasePath();
		this.maxCount = config.getInteger("scheduler.data.retention.count");
	}

	public List<Observation> findAllBySatelliteId(String satelliteId) {
		Path dataRoot = basepath.resolve(satelliteId).resolve("data");
		if (!Files.exists(dataRoot)) {
			return Collections.emptyList();
		}
		List<Path> observations;
		try {
			observations = Util.toList(Files.newDirectoryStream(dataRoot));
		} catch (IOException e) {
			LOG.error("unable to load observations", e);
			return Collections.emptyList();
		}
		Collections.sort(observations, FilenameComparator.INSTANCE_DESC);
		List<Observation> result = new ArrayList<>(observations.size());
		for (Path curDirectory : observations) {
			Observation cur = find(satelliteId, curDirectory);
			// some directories might be corrupted
			if (cur == null) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	public Observation find(String satelliteId, String observationId) {
		Path baseDirectory = basepath.resolve(satelliteId).resolve("data").resolve(observationId);
		if (!Files.exists(baseDirectory)) {
			return null;
		}
		return find(satelliteId, baseDirectory);
	}

	private static Observation find(String satelliteId, Path curDirectory) {
		Path dest = curDirectory.resolve("meta.json");
		if (!Files.exists(dest)) {
			return null;
		}
		Observation full;
		try (BufferedReader r = Files.newBufferedReader(dest)) {
			JsonObject meta = Json.parse(r).asObject();
			full = Observation.fromJson(meta);
		} catch (Exception e) {
			LOG.error("unable to load meta", e);
			return null;
		}

		Path a = curDirectory.resolve("a.jpg");
		if (Files.exists(a)) {
			full.setaPath(a.toFile());
			full.setaURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/a.jpg");
		}
		Path data = curDirectory.resolve("data.bin");
		if (Files.exists(data)) {
			full.setDataPath(data.toFile());
			full.setDataURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/data.bin");
		}
		Path wav = curDirectory.resolve(OUTPUT_WAV_FILENAME);
		if (Files.exists(wav)) {
			full.setWavPath(wav.toFile());
		}
		Path tarGz = curDirectory.resolve(OUTPUT_RAW_FILENAME);
		if (Files.exists(tarGz)) {
			full.setIqPath(tarGz.toFile());
		}
		Path spectogram = curDirectory.resolve(SPECTOGRAM_FILENAME);
		if (Files.exists(spectogram)) {
			full.setSpectogramPath(spectogram.toFile());
			full.setSpectogramURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + SPECTOGRAM_FILENAME);
		}

		return full;
	}

	public File saveImage(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve("a.jpg");
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	public File saveData(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve("data.bin");
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	public boolean saveSpectogram(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(SPECTOGRAM_FILENAME);
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return false;
		}
		return a.renameTo(dest.toFile());
	}

	public File insert(ObservationRequest observation, File dataFile) {
		try {
			Path satelliteBasePath = basepath.resolve(observation.getSatelliteId()).resolve("data");
			if (Files.exists(satelliteBasePath)) {
				List<Path> dataDirs = Util.toList(Files.newDirectoryStream(satelliteBasePath));
				if (dataDirs.size() > maxCount) {
					Collections.sort(dataDirs, FilenameComparator.INSTANCE_ASC);
					for (int i = 0; i < (dataDirs.size() - maxCount); i++) {
						Util.deleteDirectory(dataDirs.get(i));
					}
				}
			}
		} catch (IOException e) {
			LOG.error("unable to cleanup old observations", e);
		}

		Path observationBasePath = getObservationBasepath(observation);
		if (!Util.initDirectory(observationBasePath)) {
			return null;
		}

		Observation full = new Observation(observation);
		if (!update(full)) {
			return null;
		}

		return insertData(observation, dataFile);
	}

	private File insertData(ObservationRequest observation, File dataFile) {
		String filename;
		if (dataFile.getName().endsWith("wav")) {
			filename = OUTPUT_WAV_FILENAME;
		} else {
			filename = OUTPUT_RAW_FILENAME;
		}
		Path dest = getObservationBasepath(observation).resolve(filename);
		if (!Util.initDirectory(dest.getParent())) {
			return null;
		}
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!dataFile.renameTo(dest.toFile())) {
			LOG.error("unable to save file from {} to {}. Check src and dst are on the same filesystem", dataFile.getAbsolutePath(), dest.toFile().getAbsolutePath());
			return null;
		}
		return dest.toFile();
	}

	public boolean update(Observation cur) {
		JsonObject meta = cur.toJson(null);
		Path dest = getObservationBasepath(cur).resolve("meta.json");
		try (BufferedWriter w = Files.newBufferedWriter(dest)) {
			w.append(meta.toString());
			return true;
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
			return false;
		}
	}

	private Path getObservationBasepath(Observation observation) {
		return getObservationBasepath(observation.getSatelliteId(), observation.getId());
	}

	private Path getObservationBasepath(ObservationRequest observation) {
		return getObservationBasepath(observation.getSatelliteId(), observation.getId());
	}

	private Path getObservationBasepath(String satelliteId, String observationId) {
		return basepath.resolve(satelliteId).resolve("data").resolve(observationId);
	}
}

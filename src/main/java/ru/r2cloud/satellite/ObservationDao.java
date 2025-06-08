package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationComparator;
import ru.r2cloud.model.Page;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class ObservationDao implements IObservationDao {

	private static final String DEST_ALREADY_EXIST_MESSAGE = "unable to save. dest already exist: {}";
	private static final String SPECTOGRAM_FILENAME = "spectogram.png";
	private static final String DATA_FILENAME = "data.bin";
	private static final String IMAGE_FILENAME = "a.jpg";
	private static final String META_FILENAME = "meta.json";
	public static final String OUTPUT_WAV_FILENAME = "output.wav";
	public static final String OUTPUT_RAW_FILENAME_GZIPPED = "output.raw.gz";
	public static final String OUTPUT_RAW_FILENAME = "output.raw";

	private static final Logger LOG = LoggerFactory.getLogger(ObservationDao.class);
	private static final Map<String, List<Observation>> IN_FLIGHT_OBSERVATIONS = new HashMap<>();

	private final Path basepath;
	private final TimeSizeRetention retention;

	public ObservationDao(Configuration config) {
		this.basepath = config.getSatellitesBasePath();
		int maxCountRawData = config.getInteger("scheduler.data.retention.raw.count");
		Long maxRetentionSize = config.getLong("scheduler.data.retention.maxSizeBytes");
		LOG.info("retention: keep last {}Mb of observations and {} of raw IQ data", (maxRetentionSize / 1024 / 1024), maxCountRawData);
		retention = new TimeSizeRetention(maxRetentionSize, maxCountRawData, basepath);
	}

	@Override
	public List<Observation> findAll(Page page) {
		if (!Files.exists(basepath)) {
			return Collections.emptyList();
		}
		List<Observation> result;
		if (page.getSatelliteId() != null) {
			result = findAllBySatelliteId(page.getSatelliteId());
		} else {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(basepath)) {
				result = new ArrayList<>();
				for (Path curSatellite : ds) {
					result.addAll(findAllBySatelliteId(curSatellite.getFileName().toString()));
				}
			} catch (IOException e) {
				LOG.error("unable to find all", e);
				return Collections.emptyList();
			}
		}
		Collections.sort(result, ObservationComparator.INSTANCE);
		if (page.getLimit() == null && page.getCursor() == null) {
			return result;
		}
		List<Observation> sublist = new ArrayList<>();
		boolean foundStart = (page.getCursor() == null);
		for (int i = 0; i < result.size(); i++) {
			// linear search for cursor
			if (!foundStart && page.getCursor() != null) {
				foundStart = result.get(i).getId().equals(page.getCursor());
				continue;
			}
			if (page.getLimit() != null && sublist.size() >= page.getLimit()) {
				break;
			}
			sublist.add(result.get(i));
		}
		return sublist;
	}

	private List<Observation> findAllBySatelliteId(String satelliteId) {
		List<Observation> result = new ArrayList<>();
		result.addAll(loadFromDisk(satelliteId));
		synchronized (IN_FLIGHT_OBSERVATIONS) {
			List<Observation> inFlight = IN_FLIGHT_OBSERVATIONS.get(satelliteId);
			if (inFlight != null) {
				result.addAll(inFlight);
			}
		}
		return result;
	}

	private List<Observation> loadFromDisk(String satelliteId) {
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

	@Override
	public Observation find(String satelliteId, String observationId) {
		synchronized (IN_FLIGHT_OBSERVATIONS) {
			List<Observation> inFlight = IN_FLIGHT_OBSERVATIONS.get(satelliteId);
			if (inFlight != null) {
				for (Observation cur : inFlight) {
					if (cur.getId().equalsIgnoreCase(observationId)) {
						return cur;
					}
				}
			}
		}
		Path baseDirectory = basepath.resolve(satelliteId).resolve("data").resolve(observationId);
		if (!Files.exists(baseDirectory)) {
			return null;
		}
		return find(satelliteId, baseDirectory);
	}

	private static Observation find(String satelliteId, Path curDirectory) {
		Path dest = curDirectory.resolve(META_FILENAME);
		if (!Files.exists(dest)) {
			return null;
		}
		try {
			if (Files.size(dest) == 0L) {
				return null;
			}
		} catch (IOException e1) {
			return null;
		}
		Observation full;
		try (BufferedReader r = Files.newBufferedReader(dest)) {
			JsonObject meta = Json.parse(r).asObject();
			full = Observation.fromJson(meta);
		} catch (Exception e) {
			LOG.error("unable to load meta from {}", dest, e);
			return null;
		}

		Path a = curDirectory.resolve(IMAGE_FILENAME);
		if (Files.exists(a)) {
			full.setImagePath(a.toFile());
			full.setaURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + IMAGE_FILENAME);
		}
		Path data = curDirectory.resolve(DATA_FILENAME);
		if (Files.exists(data)) {
			full.setDataPath(data.toFile());
			full.setDataURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + DATA_FILENAME);
		}
		Path rawPath = resolveRawPath(curDirectory);
		if (Files.exists(rawPath)) {
			full.setRawPath(rawPath.toFile());
			full.setRawURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + rawPath.getFileName());
			full.setSigmfDataURL("/api/v1/static/observation/sigmf/data?satelliteId=" + satelliteId + "&id=" + full.getId());
			full.setSigmfMetaURL("/api/v1/static/observation/sigmf/meta?satelliteId=" + satelliteId + "&id=" + full.getId());
		}
		Path spectogram = curDirectory.resolve(SPECTOGRAM_FILENAME);
		if (Files.exists(spectogram)) {
			full.setSpectogramPath(spectogram.toFile());
			full.setSpectogramURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + SPECTOGRAM_FILENAME);
		}
		if (full.getInstruments() != null) {
			for (Instrument cur : full.getInstruments()) {
				if (cur.getChannels() != null) {
					for (InstrumentChannel curChannel : cur.getChannels()) {
						Path curPath = resolveByPrefix(curDirectory, cur.getId() + "-" + curChannel.getId() + ".");
						if (curPath == null) {
							continue;
						}
						File file = curPath.toFile();
						curChannel.setImage(file);
						curChannel.setImageURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + file.getName());
					}
				} else if (cur.isSeries()) {
					List<String> seriesUrls = new ArrayList<>();
					for (Path curPath : resolveMultipleByPrefix(curDirectory, cur.getId() + "-")) {
						seriesUrls.add("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + curPath.toFile().getName());
					}
					cur.setImageSeriesURL(seriesUrls);
				}
				Path combinedPath = resolveByPrefix(curDirectory, cur.getId() + ".");
				if (combinedPath != null) {
					File combinedFile = combinedPath.toFile();
					cur.setCombinedImage(combinedFile);
					// assume each instrument save 1 image
					cur.setCombinedImageURL("/api/v1/admin/static/satellites/" + satelliteId + "/data/" + full.getId() + "/" + combinedFile.getName());
				}
			}
		}
		return full;
	}

	private static List<Path> resolveMultipleByPrefix(Path baseDir, String prefix) {
		List<Path> result = new ArrayList<>();
		File[] files = baseDir.toFile().listFiles();
		for (File cur : files) {
			if (cur.getName().startsWith(prefix)) {
				result.add(cur.toPath());
			}
		}
		//FIXME sort by name
		return result;
	}

	private static Path resolveByPrefix(Path baseDir, String prefix) {
		File[] files = baseDir.toFile().listFiles();
		for (File cur : files) {
			if (cur.getName().startsWith(prefix)) {
				return cur.toPath();
			}
		}
		return null;
	}

	private static Path resolveRawPath(Path baseDir) {
		Path result = baseDir.resolve(OUTPUT_WAV_FILENAME);
		if (Files.exists(result)) {
			return result;
		}
		result = baseDir.resolve(OUTPUT_RAW_FILENAME_GZIPPED);
		if (Files.exists(result)) {
			return result;
		}
		return baseDir.resolve(OUTPUT_RAW_FILENAME);
	}

	@Override
	public File saveImage(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(IMAGE_FILENAME);
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	@Override
	public File saveChannel(String satelliteId, String observationId, String instrumentId, String channelId, File imagePath) {
		if (imagePath == null) {
			return null;
		}
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(instrumentId + "-" + channelId + "." + getExtension(imagePath.getName()));
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!imagePath.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	@Override
	public File saveCombined(String satelliteId, String observationId, String instrumentId, File combinedImagePath) {
		if (combinedImagePath == null) {
			return null;
		}
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(instrumentId + "." + getExtension(combinedImagePath.getName()));
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!combinedImagePath.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	@Override
	public List<File> saveImageSeries(String satelliteId, String observationId, String instrumentId, List<File> series) {
		if (series == null) {
			return Collections.emptyList();
		}
		List<File> result = new ArrayList<>(series.size());
		for (int i = 0; i < series.size(); i++) {
			File seriesImage = series.get(i);
			Path dest = getObservationBasepath(satelliteId, observationId).resolve(instrumentId + "-" + i + "." + getExtension(seriesImage.getName()));
			if (Files.exists(dest)) {
				LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
				continue;
			}
			if (!seriesImage.renameTo(dest.toFile())) {
				continue;
			}
			result.add(dest.toFile());
		}
		return result;
	}

	@Override
	public File saveData(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(DATA_FILENAME);
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	@Override
	public File saveSpectogram(String satelliteId, String observationId, File a) {
		Path dest = getObservationBasepath(satelliteId, observationId).resolve(SPECTOGRAM_FILENAME);
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!a.renameTo(dest.toFile())) {
			return null;
		}
		return dest.toFile();
	}

	@Override
	public void insert(Observation observation) {
		synchronized (IN_FLIGHT_OBSERVATIONS) {
			List<Observation> inFlight = IN_FLIGHT_OBSERVATIONS.get(observation.getSatelliteId());
			if (inFlight == null) {
				inFlight = new ArrayList<>();
				IN_FLIGHT_OBSERVATIONS.put(observation.getSatelliteId(), inFlight);
			}
			inFlight.add(observation);
		}
	}

	@Override
	public void cancel(Observation observation) {
		synchronized (IN_FLIGHT_OBSERVATIONS) {
			List<Observation> inFlight = IN_FLIGHT_OBSERVATIONS.get(observation.getSatelliteId());
			if (inFlight != null) {
				Iterator<Observation> it = inFlight.iterator();
				while (it.hasNext()) {
					Observation cur = it.next();
					if (cur.getId().equalsIgnoreCase(observation.getId())) {
						it.remove();
						break;
					}
				}
				if (inFlight.isEmpty()) {
					IN_FLIGHT_OBSERVATIONS.remove(observation.getSatelliteId());
				}
			}
		}
	}

	@Override
	public File update(Observation observation, File rawFile) {
		synchronized (IN_FLIGHT_OBSERVATIONS) {
			cancel(observation);

			Path observationBasePath = getObservationBasepath(observation);
			if (!Util.initDirectory(observationBasePath)) {
				return null;
			}

			if (!update(observation)) {
				return null;
			}
		}
		return insertRawFile(observation, rawFile);
	}

	private File insertRawFile(Observation observation, File rawFile) {
		String filename;
		if (rawFile.getName().endsWith("wav")) {
			filename = OUTPUT_WAV_FILENAME;
		} else if (rawFile.getName().endsWith(".gz")) {
			filename = OUTPUT_RAW_FILENAME_GZIPPED;
		} else {
			filename = OUTPUT_RAW_FILENAME;
		}
		Path observationBasepath = getObservationBasepath(observation);
		Path dest = observationBasepath.resolve(filename);
		if (!Util.initDirectory(dest.getParent())) {
			return null;
		}
		if (Files.exists(dest)) {
			LOG.info(DEST_ALREADY_EXIST_MESSAGE, dest.toAbsolutePath());
			return null;
		}
		if (!rawFile.renameTo(dest.toFile())) {
			LOG.error("unable to save file from {} to {}. Check src and dst are on the same filesystem", rawFile.getAbsolutePath(), dest.toFile().getAbsolutePath());
			return null;
		}
		if (retention != null) {
			// re-index and cleanup on new raw File
			// rawFile is the biggest file in the observation
			retention.indexAndCleanup(observationBasepath);
		}
		return dest.toFile();
	}

	@Override
	public boolean update(Observation cur) {
		JsonObject meta = cur.toJson(null);
		Path temp = getObservationBasepath(cur).resolve(META_FILENAME + ".tmp");
		try (BufferedWriter w = Files.newBufferedWriter(temp)) {
			w.append(meta.toString());
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
			return false;
		}
		Path dest = getObservationBasepath(cur).resolve(META_FILENAME);
		try {
			Files.move(temp, dest, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (IOException e) {
			LOG.error("unable to write meta", e);
			return false;
		}
	}

	private Path getObservationBasepath(Observation observation) {
		return getObservationBasepath(observation.getSatelliteId(), observation.getId());
	}

	private Path getObservationBasepath(String satelliteId, String observationId) {
		return basepath.resolve(satelliteId).resolve("data").resolve(observationId);
	}

	private static String getExtension(String filename) {
		int index = filename.lastIndexOf('.');
		if (index == -1) {
			return filename;
		}
		return filename.substring(index + 1);
	}

}

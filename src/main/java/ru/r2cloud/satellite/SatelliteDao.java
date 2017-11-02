package ru.r2cloud.satellite;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.r2cloud.FilenameComparator;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

import com.google.common.base.Splitter;

public class SatelliteDao {

	private static final Pattern SATELLITE_ID = Pattern.compile("(\\d+)\\((.*?)\\)");
	private final List<Satellite> satellites;
	private final File basepath;
	private final Map<String, Satellite> satelliteByName = new HashMap<String, Satellite>();

	public SatelliteDao(Configuration config) {
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		satellites = new ArrayList<Satellite>();
		for (String cur : Splitter.on(",").trimResults().omitEmptyStrings().split(config.getProperty("satellites.supported"))) {
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

	public List<ObservationResult> findWeatherObservations(Satellite satellite) {
		File dataRoot = new File(basepath, satellite.getId() + File.separator + "data");
		if (!dataRoot.exists()) {
			return Collections.emptyList();
		}
		File[] observations = dataRoot.listFiles();
		Arrays.sort(observations, FilenameComparator.INSTANCE_DESC);
		List<ObservationResult> result = new ArrayList<ObservationResult>(observations.length);
		for (File curDirectory : observations) {
			ObservationResult cur = new ObservationResult();
			cur.setDate(new Date(Long.valueOf(curDirectory.getName())));
			File a = new File(curDirectory, "a.jpg");
			if (a.exists()) {
				cur.setaPath("/admin/static/satellites/" + satellite.getId() + "/data/" + curDirectory.getName() + "/a.jpg");
			}
			File b = new File(curDirectory, "b.jpg");
			if (b.exists()) {
				cur.setbPath("/admin/static/satellites/" + satellite.getId() + "/data/" + curDirectory.getName() + "/b.jpg");
			}
			result.add(cur);
		}
		return result;
	}

}

package ru.r2cloud.satellite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationIdComparator;
import ru.r2cloud.model.Page;

public class ObservationDaoCache implements IObservationDao {

	private final IObservationDao impl;
	private final TreeMap<String, Observation> byId = new TreeMap<>(ObservationIdComparator.INSTANCE);
	private final Map<String, TreeMap<String, Observation>> bySatelliteId = new HashMap<>();

	public ObservationDaoCache(IObservationDao impl) {
		this.impl = impl;
		synchronized (byId) {
			for (Observation cur : impl.findAll(new Page())) {
				index(cur);
			}
		}
	}

	@Override
	public List<Observation> findAll(Page page) {
		synchronized (byId) {
			TreeMap<String, Observation> indexToUse;
			if (page.getSatelliteId() != null) {
				indexToUse = bySatelliteId.get(page.getSatelliteId());
				if (indexToUse == null) {
					return Collections.emptyList();
				}
			} else {
				indexToUse = byId;
			}

			NavigableMap<String, Observation> offsetIndex;
			if (page.getCursor() != null) {
				offsetIndex = indexToUse.tailMap(page.getCursor(), false);
			} else {
				offsetIndex = indexToUse;
			}

			if (page.getLimit() == null) {
				return new ArrayList<>(offsetIndex.values());
			}

			int index = 0;
			List<Observation> result = new ArrayList<>();
			for (Observation cur : offsetIndex.values()) {
				result.add(cur);
				index++;
				if (index >= page.getLimit()) {
					break;
				}
			}
			return result;
		}
	}

	private void index(Observation observation) {
		if (observation == null) {
			return;
		}
		byId.put(observation.getId(), observation);
		TreeMap<String, Observation> curSatellite = bySatelliteId.get(observation.getSatelliteId());
		if (curSatellite == null) {
			curSatellite = new TreeMap<>(ObservationIdComparator.INSTANCE);
			bySatelliteId.put(observation.getSatelliteId(), curSatellite);
		}
		curSatellite.put(observation.getId(), observation);
	}

	private void removeFromIndex(Observation observation) {
		byId.remove(observation.getId());
		TreeMap<String, Observation> curSatellite = bySatelliteId.get(observation.getSatelliteId());
		if (curSatellite == null) {
			return;
		}
		curSatellite.remove(observation.getId());
		if (curSatellite.isEmpty()) {
			bySatelliteId.remove(observation.getSatelliteId());
		}
	}

	@Override
	public Observation find(String satelliteId, String observationId) {
		synchronized (byId) {
			Observation result = byId.get(observationId);
			if (result != null) {
				return result;
			}
			result = impl.find(satelliteId, observationId);
			if (result != null) {
				index(result);
			}
			return result;
		}
	}

	@Override
	public File saveImage(String satelliteId, String observationId, File a) {
		File result = impl.saveImage(satelliteId, observationId, a);
		// if updated, then update internal cache
		if (result != null) {
			synchronized (byId) {
				index(impl.find(satelliteId, observationId));
			}
		}
		return result;
	}

	@Override
	public File saveData(String satelliteId, String observationId, File a) {
		File result = impl.saveData(satelliteId, observationId, a);
		if (result != null) {
			synchronized (byId) {
				index(impl.find(satelliteId, observationId));
			}
		}
		return result;
	}

	@Override
	public File saveSpectogram(String satelliteId, String observationId, File a) {
		File result = impl.saveSpectogram(satelliteId, observationId, a);
		if (result != null) {
			synchronized (byId) {
				index(impl.find(satelliteId, observationId));
			}
		}
		return result;
	}

	@Override
	public void insert(Observation observation) {
		impl.insert(observation);
		synchronized (byId) {
			index(observation);
		}
	}

	@Override
	public void cancel(Observation observation) {
		impl.cancel(observation);
		synchronized (byId) {
			removeFromIndex(observation);
		}
	}

	@Override
	public File update(Observation observation, File rawFile) {
		File result = impl.update(observation, rawFile);
		if (result != null) {
			synchronized (byId) {
				index(impl.find(observation.getSatelliteId(), observation.getId()));
			}
		}
		return result;
	}

	@Override
	public boolean update(Observation observation) {
		boolean result = impl.update(observation);
		synchronized (byId) {
			Observation actual = impl.find(observation.getSatelliteId(), observation.getId());
			if (actual == null) {
				removeFromIndex(observation);
			} else {
				index(actual);
			}
		}
		return result;
	}

}

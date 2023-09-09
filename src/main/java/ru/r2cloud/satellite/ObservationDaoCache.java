package ru.r2cloud.satellite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationCacheKey;

public class ObservationDaoCache implements IObservationDao {

	private final IObservationDao impl;
	private final Map<String, Observation> cacheById = new HashMap<>();
	private final Set<ObservationCacheKey> allObservations = new HashSet<>();

	public ObservationDaoCache(IObservationDao impl) {
		this.impl = impl;
		synchronized (cacheById) {
			for (Observation cur : impl.findAll()) {
				cacheById.put(cur.getId(), cur);
				allObservations.add(new ObservationCacheKey(cur.getSatelliteId(), cur.getId()));
			}
		}
	}

	@Override
	public List<Observation> findAll() {
		synchronized (cacheById) {
			List<Observation> result = new ArrayList<>(allObservations.size());
			List<ObservationCacheKey> missing = new ArrayList<>();
			for (ObservationCacheKey id : allObservations) {
				Observation cur = cacheById.get(id.getObservationId());
				if (cur == null) {
					missing.add(id);
				} else {
					result.add(cur);
				}
			}
			result.addAll(findAndIndex(missing));
			return result;
		}
	}

	@Override
	public List<Observation> findAllBySatelliteId(String satelliteId) {
		synchronized (cacheById) {
			List<Observation> result = new ArrayList<>();
			List<ObservationCacheKey> missing = new ArrayList<>();
			for (ObservationCacheKey id : allObservations) {
				if (!id.getSatelliteId().equals(satelliteId)) {
					continue;
				}
				Observation cur = cacheById.get(id.getObservationId());
				if (cur == null) {
					missing.add(id);
				} else {
					result.add(cur);
				}
			}
			result.addAll(findAndIndex(missing));
			return result;
		}
	}

	// this will load into allObservations cache
	// if observation somehow added onto disk directly
	// shouldn't happen in real life, mostly used by tests
	private Collection<? extends Observation> findAndIndex(List<ObservationCacheKey> dirty) {
		List<Observation> result = new ArrayList<>();
		List<ObservationCacheKey> missing = new ArrayList<>();
		for (ObservationCacheKey cur : dirty) {
			Observation observation = impl.find(cur.getSatelliteId(), cur.getObservationId());
			if (observation == null) {
				missing.add(cur);
				continue;
			}
			cacheById.put(observation.getId(), observation);
			allObservations.add(cur);
			result.add(observation);
		}
		allObservations.removeAll(missing);
		return result;
	}

	@Override
	public Observation find(String satelliteId, String observationId) {
		synchronized (cacheById) {
			Observation result = cacheById.get(observationId);
			if (result == null) {
				result = impl.find(satelliteId, observationId);
				if (result != null) {
					cacheById.put(observationId, result);
					// ensure indexes in allObservations
					allObservations.add(new ObservationCacheKey(satelliteId, observationId));
				}
			}
			return result;
		}
	}

	@Override
	public File saveImage(String satelliteId, String observationId, File a) {
		synchronized (cacheById) {
			cacheById.remove(observationId);
		}
		return impl.saveImage(satelliteId, observationId, a);
	}

	@Override
	public File saveData(String satelliteId, String observationId, File a) {
		synchronized (cacheById) {
			cacheById.remove(observationId);
		}
		return impl.saveData(satelliteId, observationId, a);
	}

	@Override
	public File saveSpectogram(String satelliteId, String observationId, File a) {
		synchronized (cacheById) {
			cacheById.remove(observationId);
		}
		return impl.saveSpectogram(satelliteId, observationId, a);
	}

	@Override
	public void insert(Observation observation) {
		impl.insert(observation);
		synchronized (cacheById) {
			allObservations.add(new ObservationCacheKey(observation.getSatelliteId(), observation.getId()));
		}
	}

	@Override
	public void cancel(Observation observation) {
		impl.cancel(observation);
		synchronized (cacheById) {
			cacheById.remove(observation.getId());
			allObservations.remove(new ObservationCacheKey(observation.getSatelliteId(), observation.getId()));
		}
	}

	@Override
	public File update(Observation observation, File rawFile) {
		synchronized (cacheById) {
			cacheById.remove(observation.getId());
			// update also insert new observation
			allObservations.add(new ObservationCacheKey(observation.getSatelliteId(), observation.getId()));
		}
		return impl.update(observation, rawFile);
	}

	@Override
	public boolean update(Observation cur) {
		synchronized (cacheById) {
			cacheById.remove(cur.getId());
		}
		return impl.update(cur);
	}

}

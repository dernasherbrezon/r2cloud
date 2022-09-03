package ru.r2cloud.satellite;

import java.io.File;
import java.util.List;

import ru.r2cloud.model.Observation;

public interface IObservationDao {

	List<Observation> findAll();

	List<Observation> findAllBySatelliteId(String satelliteId);

	Observation find(String satelliteId, String observationId);

	File saveImage(String satelliteId, String observationId, File a);

	File saveData(String satelliteId, String observationId, File a);

	File saveSpectogram(String satelliteId, String observationId, File a);

	void insert(Observation observation);

	void cancel(Observation observation);

	File update(Observation observation, File rawFile);

	boolean update(Observation cur);

}
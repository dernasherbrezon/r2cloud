package ru.r2cloud.satellite;

import java.io.File;
import java.util.List;

import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Page;

public interface IObservationDao {

	List<Observation> findAll(Page page);

	Observation find(String satelliteId, String observationId);

	File saveImage(String satelliteId, String observationId, File a);

	File saveData(String satelliteId, String observationId, File a);

	File saveSpectogram(String satelliteId, String observationId, File a);

	void insert(Observation observation);

	void cancel(Observation observation);

	File update(Observation observation, File iq);

	boolean update(Observation cur);

	File saveChannel(String satelliteId, String observationId, String instrumentId, String channelId, File imagePath);

	File saveCombined(String satelliteId, String observationId, String instrumentId, File combinedImagePath);

}
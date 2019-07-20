package ru.r2cloud.satellite;

import java.util.concurrent.Future;

import ru.r2cloud.model.ObservationRequest;

public class ScheduledObservation implements ScheduleEntry {

	private final ObservationRequest req;
	private final Future<?> future;
	private final Future<?> reaperFuture;
	private final Runnable completeTask;

	ScheduledObservation(ObservationRequest req, Future<?> future, Future<?> reaperFuture, Runnable completeTask) {
		this.req = req;
		this.future = future;
		this.reaperFuture = reaperFuture;
		this.completeTask = completeTask;
	}

	public ObservationRequest getReq() {
		return req;
	}

	public Future<?> getFuture() {
		return future;
	}

	public Future<?> getReaperFuture() {
		return reaperFuture;
	}

	@Override
	public String getId() {
		return req.getSatelliteId();
	}

	@Override
	public long getStartTimeMillis() {
		return req.getStartTimeMillis();
	}

	@Override
	public long getEndTimeMillis() {
		return req.getEndTimeMillis();
	}

	public Runnable getCompleteTask() {
		return completeTask;
	}

	@Override
	public void cancel() {
		if (future != null) {
			future.cancel(true);
		}
		if (reaperFuture != null) {
			reaperFuture.cancel(true);
		}
	}
}

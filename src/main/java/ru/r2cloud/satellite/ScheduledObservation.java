package ru.r2cloud.satellite;

import java.util.concurrent.Future;

import ru.r2cloud.model.ObservationRequest;

public class ScheduledObservation {

	private final ObservationRequest req;
	private final Future<?> future;
	private final Future<?> reaperFuture;

	public ScheduledObservation(ObservationRequest req, Future<?> future, Future<?> reaperFuture) {
		this.req = req;
		this.future = future;
		this.reaperFuture = reaperFuture;
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

	public void cancel() {
		if (future != null) {
			future.cancel(true);
		}
		if (reaperFuture != null) {
			reaperFuture.cancel(true);
		}
	}
}

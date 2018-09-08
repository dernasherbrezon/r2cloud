package ru.r2cloud.satellite;

import java.util.concurrent.Future;

public class ScheduledObservation {

	private final Observation observation;
	private final Future<?> future;
	private final Future<?> reaperFuture;

	public ScheduledObservation(Observation observation, Future<?> future, Future<?> reaperFuture) {
		this.observation = observation;
		this.future = future;
		this.reaperFuture = reaperFuture;
	}

	public Observation getObservation() {
		return observation;
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

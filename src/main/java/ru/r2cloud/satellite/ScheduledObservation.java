package ru.r2cloud.satellite;

import java.util.concurrent.Future;

public class ScheduledObservation {

	private final Future<?> future;
	private final Future<?> completeTaskFuture;
	private final Future<?> rotatorFuture;
	private final Runnable completeTask;

	private boolean cancelled = false;

	public ScheduledObservation(Future<?> future, Future<?> completeTaskFuture, Runnable completeTask, Future<?> rotatorFuture) {
		this.future = future;
		this.completeTaskFuture = completeTaskFuture;
		this.completeTask = completeTask;
		this.rotatorFuture = rotatorFuture;
	}

	public Future<?> getFuture() {
		return future;
	}

	public Future<?> getReaperFuture() {
		return completeTaskFuture;
	}

	public Runnable getCompleteTask() {
		return completeTask;
	}

	public void cancel() {
		if (future != null) {
			future.cancel(true);
		}
		if (completeTaskFuture != null) {
			completeTaskFuture.cancel(true);
		}
		if (completeTask != null) {
			completeTask.run();
		}
		if (rotatorFuture != null) {
			rotatorFuture.cancel(true);
		}
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}
}

package ru.r2cloud;

public class RtlSdrListener1 implements Lifecycle {

	private boolean suspended = false;
	private boolean resumed = false;

	@Override
	public void stop() {
		suspended = true;
	}

	@Override
	public void start() {
		resumed = true;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public boolean isResumed() {
		return resumed;
	}

}

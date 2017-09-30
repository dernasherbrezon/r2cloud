package ru.r2cloud;

public class RtlSdrListener1 implements RtlSdrListener {

	private boolean suspended = false;
	private boolean resumed = false;

	@Override
	public void suspend() {
		suspended = true;
	}

	@Override
	public void resume() {
		resumed = true;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public boolean isResumed() {
		return resumed;
	}

}

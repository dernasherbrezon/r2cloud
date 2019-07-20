package ru.r2cloud.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class ProcessWrapperImpl implements ProcessWrapper {

	private Process impl;

	public ProcessWrapperImpl(Process impl) {
		this.impl = impl;
	}

	@Override
	public int waitFor() throws InterruptedException {
		return impl.waitFor();
	}

	@Override
	public boolean isAlive() {
		return impl.isAlive();
	}

	@Override
	public void destroy() {
		impl.destroy();
	}

	@Override
	public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
		return impl.waitFor(timeout, unit);
	}

	@Override
	public ProcessWrapper destroyForcibly() {
		impl = impl.destroyForcibly();
		return this;
	}

	@Override
	public OutputStream getOutputStream() {
		return impl.getOutputStream();
	}
	
	@Override
	public InputStream getInputStream() {
		return impl.getInputStream();
	}
	
	@Override
	public InputStream getErrorStream() {
		return impl.getErrorStream();
	}
}

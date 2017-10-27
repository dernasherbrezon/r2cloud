package ru.r2cloud.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class ProcessWrapper {

	private Process impl;

	public ProcessWrapper(Process impl) {
		this.impl = impl;
	}

	public int waitFor() throws InterruptedException {
		return impl.waitFor();
	}

	public boolean isAlive() {
		return impl.isAlive();
	}

	public void destroy() {
		impl.destroy();
	}

	public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
		return impl.waitFor(timeout, unit);
	}

	public ProcessWrapper destroyForcibly() {
		impl = impl.destroyForcibly();
		return this;
	}

	public OutputStream getOutputStream() {
		return impl.getOutputStream();
	}
	
	public InputStream getInputStream() {
		return impl.getInputStream();
	}
}

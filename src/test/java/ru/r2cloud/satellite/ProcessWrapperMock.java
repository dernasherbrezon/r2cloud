package ru.r2cloud.satellite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.ProcessWrapper;

public class ProcessWrapperMock implements ProcessWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessWrapperMock.class);

	private final InputStream is;
	private final OutputStream os;
	private final int statusCode;

	private boolean alive;

	public ProcessWrapperMock(InputStream is, OutputStream os) {
		this(is, os, 0);
	}

	public ProcessWrapperMock(InputStream is, OutputStream os, int statusCode) {
		this.is = is;
		this.os = os;
		alive = true;
		this.statusCode = statusCode;
	}

	@Override
	public int waitFor() throws InterruptedException {
		stop();
		return statusCode;
	}

	@Override
	public boolean isAlive() {
		return alive;
	}

	@Override
	public void destroy() {
		stop();
	}

	@Override
	public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
		stop();
		return true;
	}

	@Override
	public ProcessWrapper destroyForcibly() {
		stop();
		return this;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}

	private void stop() {
		alive = false;
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				LOG.error("unable to close", e);
			}
		}
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				LOG.error("unable to close", e);
			}
		}
	}
}

package ru.r2cloud.satellite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
	private final InputStream er;
	private final int statusCode;
	private final boolean writeOnWait;

	private boolean alive;
	private File backingFile;
	private String backingFileExtension;

	public ProcessWrapperMock(InputStream is, OutputStream os) {
		this(is, os, 0);
	}

	public ProcessWrapperMock(InputStream is, OutputStream os, int statusCode) {
		this(is, os, statusCode, false);
	}

	public ProcessWrapperMock(InputStream is, OutputStream os, int statusCode, boolean writeOnWait) {
		this(is, os, null, statusCode, writeOnWait);
	}

	public ProcessWrapperMock(InputStream is, OutputStream os, InputStream er, int statusCode, boolean writeOnWait) {
		this.is = is;
		this.os = os;
		this.er = er;
		alive = true;
		this.statusCode = statusCode;
		this.writeOnWait = writeOnWait;
	}

	@Override
	public int waitFor() throws InterruptedException {
		if (writeOnWait) {
			try {
				getOutputStream().write(1);
				getOutputStream().close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
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
		if (backingFile != null) {
			try {
				return new FileOutputStream(backingFile);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return os;
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public InputStream getErrorStream() {
		return er;
	}

	public void setBackingFile(File backingFile) {
		if (backingFileExtension != null) {
			this.backingFile = new File(backingFile.getAbsolutePath() + backingFileExtension);
		} else {
			this.backingFile = backingFile;
		}
	}
	
	public void setBackingFileExtension(String backingFileExtension) {
		this.backingFileExtension = backingFileExtension;
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

package ru.r2cloud.satellite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.SafeRunnable;

import com.google.common.io.ByteStreams;

class CopyData extends SafeRunnable {

	private static final Logger LOG = LoggerFactory.getLogger(CopyData.class);

	private final InputStream from;
	private final OutputStream to;

	CopyData(InputStream from, OutputStream to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public void doRun() {
		try {
			ByteStreams.copy(from, to);
		} catch (IOException e) {
			LOG.error("unable to copy data", e);
		}
	}

}

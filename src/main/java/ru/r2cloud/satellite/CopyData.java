package ru.r2cloud.satellite;

import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CopyData extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(CopyData.class);
	private static final int BUF_SIZE = 0x1000; // 4K

	private final InputStream from;
	private final OutputStream to;

	CopyData(InputStream from, OutputStream to) {
		super("pipe-thread");
		this.from = from;
		this.to = to;
	}

	@Override
	public void run() {
		try {
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = from.read(buf);
				if (r == -1) {
					break;
				}
				to.write(buf, 0, r);
			}
			to.flush();
		} catch (Exception e) {
			LOG.error("unable to copy data", e);
		}
	}

	public boolean shutdown() {
		this.interrupt();
		try {
			this.join(10000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return !this.isAlive();
	}

}

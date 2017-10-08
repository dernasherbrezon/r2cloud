package ru.r2cloud.satellite;

import java.io.IOException;
import java.io.InputStream;

public class BlockingStream extends InputStream {

	private Byte data = -1;

	@Override
	public int read() throws IOException {
		synchronized (this.data) {
			if (this.data == -1) {
				try {
					this.data.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return -1;
				}
			}
			byte result = this.data;
			this.data = -1;
			return result;
		}
	}

	@Override
	public int available() throws IOException {
		return Integer.MAX_VALUE;
	}

	public void setData(byte data) {
		synchronized (this.data) {
			this.data.notifyAll();
			this.data = data;
		}
	}

}

package ru.r2cloud.lora.loraat;

import java.io.IOException;
import java.io.InputStream;

public class MultiStreamInputStream extends InputStream {

	private final InputStream[] impl;
	private int current = 0;

	public MultiStreamInputStream(String... paths) {
		impl = new InputStream[paths.length];
		for (int i = 0; i < paths.length; i++) {
			impl[i] = MultiStreamInputStream.class.getClassLoader().getResourceAsStream(paths[i]);
		}
	}

	@Override
	public int read() throws IOException {
		return impl[current].read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return impl[current].read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return impl[current].read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		impl[current].close();
		current++;
	}

}

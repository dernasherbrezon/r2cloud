package ru.r2cloud.util;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdOutputStream;

public class ZiqOutputStream extends OutputStream implements Closeable {

	private DataOutputStream dos;

	public ZiqOutputStream(OutputStream os, boolean compressed, int sampleSizeBits, long sampleRate) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.write(ZiqInputStream.MAGIC);
		if (compressed) {
			dos.write(1);
		} else {
			dos.write(0);
		}
		dos.writeByte(sampleSizeBits);
		dos.writeLong(sampleRate);
		dos.writeLong(0); // no annotations
		if (compressed) {
			this.dos = new DataOutputStream(new ZstdOutputStream(os));
		} else {
			this.dos = dos;
		}
	}

	@Override
	public void write(int b) throws IOException {
		dos.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		dos.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		dos.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		dos.close();
	}
}

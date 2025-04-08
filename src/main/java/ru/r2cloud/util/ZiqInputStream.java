package ru.r2cloud.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.github.luben.zstd.ZstdInputStream;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;

public class ZiqInputStream extends InputStream implements FloatInput {

	static final byte[] MAGIC = "ZIQ_".getBytes(StandardCharsets.US_ASCII);
	private final DataInputStream dis;
	private long framePos = 0;
	private final Context context;
	private final byte[] buffer = new byte[8192];
	private int maxSize = buffer.length;
	private int currentPosition = maxSize;

	public ZiqInputStream(File file, long sampleRate, Long totalSamples) throws IOException {
		this(new BufferedInputStream(new FileInputStream(file)), sampleRate, totalSamples);
	}

	public ZiqInputStream(InputStream impl, long sampleRate, Long totalSamples) throws IOException {
		DataInputStream dis = new DataInputStream(impl);
		byte[] magic = new byte[MAGIC.length];
		dis.readFully(magic);
		if (Arrays.compare(magic, MAGIC) != 0) {
			throw new IOException("not a ziq file");
		}
		boolean isCompressed = dis.readBoolean();
		this.context = new Context();
		context.setChannels(2);
		context.setSampleSizeInBits(dis.readUnsignedByte());
		long actualSampleRate = dis.readLong();
		boolean bigEndian;
		// ziq uses whatever operating system endianess have
		if (sampleRate == actualSampleRate) {
			bigEndian = true; // that's because DataInputStream is big-endian
		} else {
			bigEndian = false;
		}
		context.setSampleRate(sampleRate);
		context.setTotalSamples(totalSamples);
		context.setCurrentSample(() -> framePos / context.getChannels());
		long jsonAnnotationsLength;
		if (bigEndian) {
			jsonAnnotationsLength = dis.readLong();
		} else {
			jsonAnnotationsLength = readLittleEndian(dis);
		}

		dis.skip(jsonAnnotationsLength); // not used
		if (isCompressed) {
			ZstdInputStream zis = new ZstdInputStream(impl);
			zis.setContinuous(true);
			this.dis = new DataInputStream(zis);
		} else {
			this.dis = dis;
		}
	}

	private static long readLittleEndian(DataInputStream dis) throws IOException {
		byte[] readBuffer = new byte[8];
		dis.readFully(readBuffer, 0, 8);
		return ((long) readBuffer[7] << 56) + ((long) (readBuffer[6] & 255) << 48) + ((long) (readBuffer[5] & 255) << 40) + ((long) (readBuffer[4] & 255) << 32) + ((long) (readBuffer[3] & 255) << 24) + ((readBuffer[2] & 255) << 16) + ((readBuffer[1] & 255) << 8) + (readBuffer[0] & 255);

	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public int read() throws IOException {
		return dis.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return dis.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return dis.read(b, off, len);
	}

	@Override
	public float readFloat() throws IOException {
		float result;
		if (currentPosition >= maxSize) {
			maxSize = dis.read(buffer);
			currentPosition = 0;
			if (maxSize == -1) {
				throw new EOFException();
			}
		}
		if (context.getSampleSizeInBits() == 8) {
			result = buffer[currentPosition] / 128.0f;
		} else {
			throw new IOException("unsupported sample size: " + context.getSampleSizeInBits());
		}
		framePos++;
		currentPosition++;
		return result;
	}

	@Override
	public void close() throws IOException {
		dis.close();
	}

}

package ru.r2cloud.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.github.luben.zstd.ZstdInputStream;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;

public class ZiqInputStream implements FloatInput {

	static final byte[] MAGIC = "ZIQ_".getBytes(StandardCharsets.US_ASCII);
	private final DataInputStream dis;
	private long framePos = 0;
	private final Context context;
	private float[] lookupTable;

	public ZiqInputStream(File file, Long totalSamples) throws IOException {
		this(new BufferedInputStream(new FileInputStream(file)), totalSamples);
	}

	public ZiqInputStream(InputStream impl, Long totalSamples) throws IOException {
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
		context.setSampleRate(dis.readLong());
		context.setTotalSamples(totalSamples);
		context.setCurrentSample(() -> framePos / context.getChannels());
		long jsonAnnotationsLength = dis.readLong();
		dis.skip(jsonAnnotationsLength); // not used
		if (context.getSampleSizeInBits() == 8) {
			lookupTable = new float[0x100];
			for (int i = 0; i < lookupTable.length; ++i) {
				lookupTable[i] = (i - 127.5f) / 128.0f;
			}
		} else {
			throw new IOException("unsupported sample size: " + context.getSampleSizeInBits());
		}
		if (isCompressed) {
			this.dis = new DataInputStream(new ZstdInputStream(impl));
		} else {
			this.dis = dis;
		}
	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public float readFloat() throws IOException {
		float result;
		if (context.getSampleSizeInBits() == 8) {
			result = lookupTable[dis.readUnsignedByte()];
		} else {
			throw new IOException("unsupported sample size: " + context.getSampleSizeInBits());
		}
		framePos++;
		return result;
	}

	@Override
	public void close() throws IOException {
		dis.close();
	}
}

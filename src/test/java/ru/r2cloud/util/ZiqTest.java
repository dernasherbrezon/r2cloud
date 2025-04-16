package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.junit.Test;

import com.github.luben.zstd.ZstdOutputStream;

public class ZiqTest {

	@Test
	public void testSuccess() throws Exception {
		runTest(true);
		runTest(false);
	}

	// ziq uses native endianness
	@Test
	public void testLittleEndian() throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40 };
		int sampleRate = 48000;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.write(ZiqInputStream.MAGIC);
		dos.write(1);
		dos.writeByte(8);
		writeLittleEndianLong(dos, sampleRate);
		writeLittleEndianLong(dos, 4); // some annotations
		dos.write(new byte[4]);
		dos = new DataOutputStream(new ZstdOutputStream(dos));
		dos.write(data);
		dos.close();
		assertResult(data, baos);
	}

	private static void runTest(boolean compressed) throws Exception, IOException {
		byte[] data = new byte[] { 10, 20, 30, 40 };
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZiqOutputStream os = new ZiqOutputStream(baos, compressed, 8, 48000);
		os.write(data);
		os.close();
		assertResult(data, baos);
	}

	private static void assertResult(byte[] data, ByteArrayOutputStream baos) throws IOException {
		ZiqInputStream is = new ZiqInputStream(new ByteArrayInputStream(baos.toByteArray()), 48000, (long) (data.length / 2));
		assertEquals(0.078125, is.readFloat(), 0.0000001);
		assertEquals(0, is.getContext().getCurrentSample().getValue());
		assertEquals(0.15625, is.readFloat(), 0.0000001);
		assertEquals(1, is.getContext().getCurrentSample().getValue());
		assertEquals(0.234375, is.readFloat(), 0.0000001);
		assertEquals(1, is.getContext().getCurrentSample().getValue());
		assertEquals(0.3125, is.readFloat(), 0.0000001);
		assertEquals(2, is.getContext().getCurrentSample().getValue());
		try {
			is.readFloat();
			fail("eof expected");
		} catch (EOFException e) {
			// do nothing
		}
	}

	private static void writeLittleEndianLong(DataOutputStream dos, long v) throws IOException {
		dos.write((byte) (v >>> 0));
		dos.write((byte) (v >>> 8));
		dos.write((byte) (v >>> 16));
		dos.write((byte) (v >>> 24));
		dos.write((byte) (v >>> 32));
		dos.write((byte) (v >>> 40));
		dos.write((byte) (v >>> 48));
		dos.write((byte) (v >>> 56));
	}

}

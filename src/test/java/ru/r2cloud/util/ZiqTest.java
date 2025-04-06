package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.junit.Test;

public class ZiqTest {

	@Test
	public void testSuccess() throws Exception {
		runTest(true);
		runTest(false);
	}

	private static void runTest(boolean compressed) throws Exception, IOException {
		byte[] data = new byte[] { 10, 20, 30, 40 };
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZiqOutputStream os = new ZiqOutputStream(baos, compressed, 8, 48000);
		os.write(data);
		os.close();
		ZiqInputStream is = new ZiqInputStream(new ByteArrayInputStream(baos.toByteArray()), (long) (data.length / 2));
		assertEquals(-0.91796875, is.readFloat(), 0.0000001);
		assertEquals(0, is.getContext().getCurrentSample().getValue());
		assertEquals(-0.83984375, is.readFloat(), 0.0000001);
		assertEquals(1, is.getContext().getCurrentSample().getValue());
		assertEquals(-0.76171875, is.readFloat(), 0.0000001);
		assertEquals(1, is.getContext().getCurrentSample().getValue());
		assertEquals(-0.68359375, is.readFloat(), 0.0000001);
		assertEquals(2, is.getContext().getCurrentSample().getValue());
		try {
			is.readFloat();
			fail("eof expected");
		} catch (EOFException e) {
			// do nothing
		}
	}

}

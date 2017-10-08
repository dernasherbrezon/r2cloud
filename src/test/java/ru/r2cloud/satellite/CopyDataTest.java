package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class CopyDataTest {

	@Test
	public void testLifecycle() throws Exception {
		BlockingStream from = new BlockingStream();
		ByteArrayOutputStream to = new ByteArrayOutputStream();
		CopyData data = new CopyData(from, to);
		data.start();
		from.setData((byte) 10);
		Thread.sleep(100);
		assertTrue(data.shutdown());
		assertEquals((byte) 10, to.toByteArray()[0]);
	}

}

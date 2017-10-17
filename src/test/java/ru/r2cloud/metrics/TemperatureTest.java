package ru.r2cloud.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TemperatureTest {

	private File tempfile;
	private Temperature temp;

	@Test
	public void testNotAvailable() {
		assertFalse(temp.isAvailable());
	}

	@Test
	public void testTemperature() throws Exception {
		try (BufferedWriter w = new BufferedWriter(new FileWriter(tempfile))) {
			w.write("47078");
		}
		assertTrue(temp.isAvailable());
		assertEquals(47.078, temp.getValue(), 0.0);
	}

	@Before
	public void start() {
		tempfile = new File("./target/" + UUID.randomUUID().toString());
		temp = new Temperature(tempfile.getAbsolutePath());
	}

	@After
	public void stop() {
		if (tempfile.exists()) {
			tempfile.delete();
		}
	}

}

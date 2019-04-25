package ru.r2cloud.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

public class TemperatureTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private MockFileSystem fs;

	private Path tempfile;
	private Temperature temp;

	@Test
	public void testNotAvailable() {
		assertFalse(temp.isAvailable());
	}

	@Test
	public void testTemperature() throws Exception {
		setupData("47078");
		assertTrue(temp.isAvailable());
		assertEquals(47.078, temp.getValue(), 0.0);
	}

	@Test
	public void testCorruptedFile() throws Exception {
		setupData("47078");
		fs.mock(tempfile, new FailingByteChannelCallback(3));
		assertNull(temp.getValue());
	}
	
	@Test
	public void testInvalidData() throws Exception {
		setupData("test");
		assertNull(temp.getValue());
	}

	@Before
	public void start() throws Exception {
		fs = new MockFileSystem(FileSystems.getDefault());
		tempfile = fs.getPath(tempFolder.getRoot().getAbsolutePath(), UUID.randomUUID().toString());
		temp = new Temperature(tempfile);
	}

	private void setupData(String data) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(tempfile)) {
			w.write(data);
		}
	}
}

package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.FixedClock;
import ru.r2cloud.MockServer;
import ru.r2cloud.TestConfiguration;

public class PriorityServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private PriorityService service;
	private TestConfiguration config;
	private MockFileSystem fs;
	private String fileLocation;
	private MockServer mockServer;

	@Test
	public void testLifecycle() throws Exception {
		try (BufferedWriter w = new BufferedWriter(new FileWriter(fileLocation))) {
			w.append("40069");
			w.newLine();
		}
		service = new PriorityService(config, new FixedClock(System.currentTimeMillis() + 3600000 * 2));
		assertEquals(1, service.find("40069").intValue());
		assertNull(service.find(UUID.randomUUID().toString()));
		mockServer.mockResponse("/priorities", "48898\n40069\n");
		service.reload();
		assertEquals(1, service.find("40069").intValue());
		assertEquals(2, service.find("48898").intValue());
		
		service = new PriorityService(config, new FixedClock(System.currentTimeMillis() + 3600000 * 2));
		fs.mock(fs.getPath(fileLocation).getParent(), new FailingByteChannelCallback(3));
		mockServer.mockResponse("/priorities", "40069\n48898\n");
		service.reload();
		
		fs.removeMock(fs.getPath(fileLocation).getParent());

		// reload from file 
		service = new PriorityService(config, new FixedClock(System.currentTimeMillis() + 3600000 * 2));
		assertEquals(1, service.find("40069").intValue());
		assertEquals(2, service.find("48898").intValue());
	}

	@Before
	public void start() throws Exception {
		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
		mockServer = new MockServer();
		mockServer.start();
		fileLocation = new File(tempFolder.getRoot(), "priorities.txt").getAbsolutePath();
		config.setProperty("satellites.priority.location", fileLocation);
		config.setProperty("satellites.priority.url", mockServer.getUrl() + "/priorities");
	}

	@After
	public void stop() {
		if (mockServer != null) {
			mockServer.stop();
		}
	}
}

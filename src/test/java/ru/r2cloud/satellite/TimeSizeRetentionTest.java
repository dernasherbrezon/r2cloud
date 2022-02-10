package ru.r2cloud.satellite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeSizeRetentionTest {

	private static final Logger LOG = LoggerFactory.getLogger(TimeSizeRetentionTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testDeleteEmptySatelliteDir() throws Exception {
		File satelliteDir = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		if (!satelliteDir.exists() && !satelliteDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + satelliteDir.getAbsolutePath());
		}
		File file = new File(satelliteDir, "tle.txt");
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(new byte[10]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		new TimeSizeRetention(22, tempFolder.getRoot().toPath());
		assertFalse(satelliteDir.exists());
	}

	@Test
	public void testCleanupOnStartup() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 15, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 15, currentTime + 1000);
		new TimeSizeRetention(22, tempFolder.getRoot().toPath());
		assertFalse(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
	}

	@Test
	public void testRetention() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		TimeSizeRetention retention = new TimeSizeRetention(22, tempFolder.getRoot().toPath());
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		Path folder3 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 2000);
		retention.indexAndCleanup(folder1);
		retention.indexAndCleanup(folder2);
		assertTrue(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
		retention.indexAndCleanup(folder3);
		assertFalse(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
		assertTrue(Files.exists(folder3));
	}

	@Test
	public void testUpdateFolderContents() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		TimeSizeRetention retention = new TimeSizeRetention(22, tempFolder.getRoot().toPath());
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		retention.indexAndCleanup(folder1);
		retention.indexAndCleanup(folder2);
		assertTrue(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
		// increase contents of folder 2 so that total size should overflow
		try (FileOutputStream fos = new FileOutputStream(new File(folder2.toFile(), UUID.randomUUID().toString()))) {
			fos.write(new byte[3]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		retention.indexAndCleanup(folder2);
		assertFalse(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
	}

	private Path createObservationFolder(String satelliteName, int size, long time) {
		File baseDir = new File(tempFolder.getRoot(), satelliteName);
		if (!baseDir.exists() && !baseDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + baseDir.getAbsolutePath());
		}
		File dataDir = new File(baseDir, "data");
		if (!dataDir.exists() && !dataDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + dataDir.getAbsolutePath());
		}
		File observationDir = new File(dataDir, UUID.randomUUID().toString());
		if (!observationDir.exists() && !observationDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + dataDir.getAbsolutePath());
		}
		File file = new File(observationDir, UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(new byte[size]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		if (!file.setLastModified(time)) {
			LOG.error("unable to setup time: {}", time);
		}
		return observationDir.toPath();
	}

}

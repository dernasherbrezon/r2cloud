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
	public void testRetention() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		Path folder1 = createFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		Path folder3 = createFolder(UUID.randomUUID().toString(), 10, currentTime + 2000);
		TimeSizeRetention retention = new TimeSizeRetention(22);
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
		Path folder1 = createFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		TimeSizeRetention retention = new TimeSizeRetention(22);
		retention.indexAndCleanup(folder1);
		retention.indexAndCleanup(folder2);
		assertTrue(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
		// increase contents of folder 2 so that total size should overflow
		try (FileOutputStream fos = new FileOutputStream(new File(folder2.toFile(), UUID.randomUUID().toString()))) {
			fos.write(new byte[2]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		retention.indexAndCleanup(folder2);
		assertFalse(Files.exists(folder1));
		assertTrue(Files.exists(folder2));
	}

	private Path createFolder(String name, int size, long time) {
		File baseDir = new File(tempFolder.getRoot(), name);
		if (!baseDir.exists() && !baseDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + baseDir.getAbsolutePath());
		}
		File file = new File(baseDir, UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(new byte[size]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		if (!file.setLastModified(time)) {
			LOG.error("unable to setup time: {}", time);
		}
		return baseDir.toPath();
	}

}

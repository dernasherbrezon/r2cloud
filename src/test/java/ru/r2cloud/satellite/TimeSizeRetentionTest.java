package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Util;

public class TimeSizeRetentionTest {

	private static final int TLE_FILE_SIZE = 10;
	private static final Logger LOG = LoggerFactory.getLogger(TimeSizeRetentionTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testDeleteRawIqFiles() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		TimeSizeRetention retention = new TimeSizeRetention(28, 2, tempFolder.getRoot().toPath());
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 10, 2, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 10, 2, currentTime + 1000);
		Path folder3 = createObservationFolder(UUID.randomUUID().toString(), 10, 2, currentTime + 2000);
		Path folder4 = createObservationFolder(UUID.randomUUID().toString(), 10, 2, currentTime + 3000);
		Path folder5 = createObservationFolder(UUID.randomUUID().toString(), 10, 2, currentTime + 4000);

		retention.indexAndCleanup(getObservationFolder(folder1));
		assertFolder(true, true, true, folder1);
		retention.indexAndCleanup(getObservationFolder(folder2));
		assertFolder(true, true, true, folder2);
		retention.indexAndCleanup(getObservationFolder(folder3));
		assertFolder(true, false, true, folder1);
		assertFolder(true, true, true, folder3);
		retention.indexAndCleanup(getObservationFolder(folder4));
		assertFolder(true, false, true, folder2);
		assertFolder(true, true, true, folder4);
		retention.indexAndCleanup(getObservationFolder(folder5));
		assertFolder(false, folder1);
		assertFolder(true, false, true, folder3);
		assertFolder(true, true, true, folder5);
	}

	@Test
	public void testRetentionOnlyForDataDirs() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		Path satelliteDir = createObservationFolder(UUID.randomUUID().toString(), 0, 0, currentTime);
		new TimeSizeRetention(TLE_FILE_SIZE - 1, 10, tempFolder.getRoot().toPath());
		// even if total satellites directories contain more than allowed, only
		// observation data is subject to retention algorithms
		assertTrue(Files.exists(satelliteDir));
	}

	@Test
	public void testCleanupOnStartup() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 15, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 15, currentTime + 1000);
		new TimeSizeRetention(22, 2, tempFolder.getRoot().toPath());
		assertFolder(false, folder1);
		assertFolder(true, folder2);
	}

	@Test
	public void testRetention() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		TimeSizeRetention retention = new TimeSizeRetention(22, 3, tempFolder.getRoot().toPath());
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		Path folder3 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 2000);
		retention.indexAndCleanup(getObservationFolder(folder1));
		retention.indexAndCleanup(getObservationFolder(folder2));
		assertFolder(true, folder1);
		assertFolder(true, folder2);
		retention.indexAndCleanup(getObservationFolder(folder3));
		assertFolder(false, folder1);
		assertFolder(true, folder2);
		assertFolder(true, folder3);
	}

	@Test
	public void testUpdateFolderContents() throws Exception {
		long currentTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
		TimeSizeRetention retention = new TimeSizeRetention(22, 2, tempFolder.getRoot().toPath());
		Path folder1 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime);
		Path folder2 = createObservationFolder(UUID.randomUUID().toString(), 10, currentTime + 1000);
		retention.indexAndCleanup(getObservationFolder(folder1));
		retention.indexAndCleanup(getObservationFolder(folder2));
		assertFolder(true, folder1);
		assertFolder(true, folder2);
		// increase contents of folder 2 so that total size should overflow
		try (FileOutputStream fos = new FileOutputStream(new File(getObservationFolder(folder2).toFile(), UUID.randomUUID().toString()))) {
			fos.write(new byte[3]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		retention.indexAndCleanup(getObservationFolder(folder2));
		assertFolder(false, folder1);
		assertFolder(true, folder2);
	}

	private static Path getObservationFolder(Path satelliteFolder) throws Exception {
		List<Path> observations = Util.toList(Files.newDirectoryStream(satelliteFolder.resolve("data")));
		assertEquals(1, observations.size());
		return observations.get(0);
	}

	private static void assertFolder(boolean expectedFolder, Path folder) {
		assertFolder(expectedFolder, true, false, folder);
	}

	private static void assertFolder(boolean expectedFolder, boolean expectedRaw, boolean expectedData, Path folder) {
		List<Path> observations;
		try {
			observations = Util.toList(Files.newDirectoryStream(folder.resolve("data")));
		} catch (IOException e) {
			e.printStackTrace();
			fail("unable to get list of directories " + e.getMessage());
			return;
		}
		if (expectedFolder) {
			assertEquals(1, observations.size());
		} else {
			assertEquals(0, observations.size());
			return;
		}
		assertEquals(expectedRaw, Files.exists(observations.get(0).resolve("output.raw")));
		assertEquals(expectedData, Files.exists(observations.get(0).resolve("data.bin")));
	}

	private Path createObservationFolder(String satelliteName, int sizeRaw, long time) {
		return createObservationFolder(satelliteName, sizeRaw, 0, time);
	}

	private Path createObservationFolder(String satelliteName, int sizeRaw, int sizeData, long time) {
		File baseDir = new File(tempFolder.getRoot(), satelliteName);
		if (!baseDir.exists() && !baseDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + baseDir.getAbsolutePath());
		}
		// can be tle.txt
		try (FileOutputStream fos = new FileOutputStream(new File(baseDir, UUID.randomUUID().toString()))) {
			fos.write(new byte[TLE_FILE_SIZE]);
		} catch (Exception e) {
			fail("unable to write: " + e.getMessage());
		}
		File dataDir = new File(baseDir, "data");
		if (!dataDir.exists() && !dataDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + dataDir.getAbsolutePath());
		}
		File observationDir = new File(dataDir, UUID.randomUUID().toString());
		if (!observationDir.exists() && !observationDir.mkdirs()) {
			throw new RuntimeException("unable to create dir: " + dataDir.getAbsolutePath());
		}
		if (sizeRaw != 0) {
			File file = new File(observationDir, "output.raw");
			try (FileOutputStream fos = new FileOutputStream(file)) {
				fos.write(new byte[sizeRaw]);
			} catch (Exception e) {
				fail("unable to write: " + e.getMessage());
			}
			if (!file.setLastModified(time)) {
				LOG.error("unable to setup time: {}", time);
			}
		}
		if (sizeData != 0) {
			File data = new File(observationDir, "data.bin");
			try (FileOutputStream fos = new FileOutputStream(data)) {
				fos.write(new byte[sizeData]);
			} catch (Exception e) {
				fail("unable to write: " + e.getMessage());
			}
			if (!data.setLastModified(time)) {
				LOG.error("unable to setup time: {}", time);
			}
		}
		return baseDir.toPath();
	}

}

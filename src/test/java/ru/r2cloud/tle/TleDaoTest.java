package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Tle;

public class TleDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TleDao dao;
	private TestConfiguration config;
	private MockFileSystem fs;
	private String fileLocation;

	@Test
	public void testLoadEmpty() {
		Map<String, Tle> result = dao.loadTle();
		assertTrue(result.isEmpty());
	}

	@Test
	public void testSaveLoadAndFail() {
		Map<String, Tle> tle = new HashMap<>();
		// TleDao doesn't verify Tle format
		tle.put(UUID.randomUUID().toString(), new Tle(new String[] { UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString() }));

		dao.saveTle(tle);

		Map<String, Tle> actual = dao.loadTle();
		assertEquals(1, actual.size());

		// simulate failure to save
		Path failingPath = fs.getPath(fileLocation).getParent();
		fs.mock(failingPath, new FailingByteChannelCallback(10));
		tle.put(UUID.randomUUID().toString(), new Tle(new String[] { UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString() }));
		dao.saveTle(tle);
		fs.removeMock(failingPath);

		// the new value cached in-memory
		actual = dao.loadTle();
		assertEquals(2, actual.size());

		// old disk cached value exist
		dao = new TleDao(config);
		actual = dao.loadTle();
		assertEquals(1, actual.size());
	}

	@Before
	public void start() throws Exception {
		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
		fileLocation = new File(tempFolder.getRoot(), "tle.txt").getAbsolutePath();
		config.setProperty("tle.cacheFileLocation", fileLocation);
		dao = new TleDao(config);
	}

}

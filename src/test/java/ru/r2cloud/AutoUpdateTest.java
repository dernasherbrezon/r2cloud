package ru.r2cloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AutoUpdateTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private AutoUpdate service;
	private TestConfiguration config;

	@Test
	public void testSuccess() {
		service.setEnabled(true);
		assertTrue(service.isEnabled());
		service.setEnabled(false);
		assertFalse(service.isEnabled());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("auto.update.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		service = new AutoUpdate(config);
	}

}

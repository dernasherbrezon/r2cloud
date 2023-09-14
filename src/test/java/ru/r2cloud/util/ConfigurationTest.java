package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.TestConfiguration;

public class ConfigurationTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private MockFileSystem fs;

	@Test
	public void notCalled() {
		ConfigListener listener = mock(ConfigListener.class);
		Integer value = new Random().nextInt();
		config.subscribe(listener, UUID.randomUUID().toString());
		config.setProperty(UUID.randomUUID().toString(), value);
		config.setProperty(UUID.randomUUID().toString(), value);
		config.update();

		verify(listener, never()).onConfigUpdated();
	}

	@Test
	public void listenToUpdate() {
		ConfigListener listener = mock(ConfigListener.class);
		String propName = UUID.randomUUID().toString();
		Integer value = new Random().nextInt();

		config.subscribe(listener, propName);
		config.setProperty(propName, value);
		config.setProperty(UUID.randomUUID().toString(), value);
		config.update();

		verify(listener, atLeast(1)).onConfigUpdated();
	}

	@Test
	public void twoListenersUpdated() {
		ConfigListener listener1 = mock(ConfigListener.class);
		ConfigListener listener2 = mock(ConfigListener.class);
		String propName = UUID.randomUUID().toString();
		Integer value = new Random().nextInt();

		config.subscribe(listener1, propName);
		config.subscribe(listener2, propName);
		config.setProperty(propName, value);
		config.setProperty(UUID.randomUUID().toString(), value);
		config.update();

		verify(listener1, atLeast(1)).onConfigUpdated();
		verify(listener2, atLeast(1)).onConfigUpdated();
	}

	@Test
	public void update() {
		String propName = UUID.randomUUID().toString();
		Integer value = new Random().nextInt();
		config.setProperty(propName, value);
		config.update();
		assertEquals(value, config.getInteger(propName));
	}

	@Test
	public void testLoad() {
		assertEquals(Integer.valueOf(8), config.getInteger("scheduler.elevation.min"));
	}
	
	@Test
	public void testNoUpdateIfNothingChanged() throws Exception {
		fs.mock(config.getTempDirectoryPath(), new FailingByteChannelCallback(3));
		Path userParentPath = fs.getPath(TestConfiguration.getUserSettingsLocation(tempFolder)).getParent();
		fs.mock(userParentPath, new FailingByteChannelCallback(3));
		config.update();
	}

	@Test
	public void testCorruptedAfterFailedWrite() throws Exception {
		String lat = "53.40";
		config.setProperty("locaiton.lat", lat);
		config.update();

		fs.mock(config.getTempDirectoryPath(), new FailingByteChannelCallback(3));
		Path userParentPath = fs.getPath(TestConfiguration.getUserSettingsLocation(tempFolder)).getParent();
		fs.mock(userParentPath, new FailingByteChannelCallback(3));

		String newLat = "23.40";
		config.setProperty("locaiton.lat", newLat);
		try {
			config.update();
			fail("config should not be updated");
		} catch (Exception e) {
			// expected
		}

		fs.removeMock(config.getTempDirectoryPath());
		fs.removeMock(userParentPath);

		config = new TestConfiguration(tempFolder, fs);
		assertEquals(lat, config.getProperty("locaiton.lat"));
	}

	@Test
	public void testRemoveValueInUserConfigs() throws Exception {
		File systemSettings = new File(tempFolder.getRoot(), "system.props");
		String propertyName = UUID.randomUUID().toString();
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(systemSettings))) {
			Properties props = new Properties();
			props.setProperty(propertyName, "1");
			props.store(os, "comments");
		}
		File userSettings = new File(tempFolder.getRoot(), "user.props");
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(userSettings))) {
			Properties props = new Properties();
			props.setProperty(propertyName, "");
			props.store(os, "comments");
		}
		try (InputStream is = new BufferedInputStream(new FileInputStream(systemSettings))) {
			Configuration config = new Configuration(is, userSettings.getAbsolutePath(), fs);
			assertNull(config.getProperty(propertyName));
		}
	}

	@Before
	public void start() throws Exception {
		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
	}

}

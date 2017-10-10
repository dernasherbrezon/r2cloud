package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTest {

	private final File userSettings = new File("./target/user.properties");
	private Configuration config;

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
	public void update() {
		String propName = UUID.randomUUID().toString();
		Integer value = new Random().nextInt();
		config.setProperty(propName, value);
		config.update();
		assertEquals(value, config.getInteger(propName));
	}

	@Test
	public void testLoad() {
		assertEquals(new Integer(8), config.getInteger("scheduler.elevation.min"));
	}

	@Before
	public void start() {
		config = new Configuration("./src/test/resources/test.properties", userSettings.getAbsolutePath());
	}

	@After
	public void stop() {
		if (userSettings.exists()) {
			userSettings.delete();
		}
	}

}

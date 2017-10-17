package ru.r2cloud;

import java.io.File;

import ru.r2cloud.util.Configuration;

public class TestConfiguration extends Configuration {

	private static final File userSettings = new File("./target/user.properties");

	public TestConfiguration() {
		super("./src/test/resources/test.properties", userSettings.getAbsolutePath());
	}

	public void stop() {
		if (userSettings.exists()) {
			userSettings.delete();
		}
	}

}

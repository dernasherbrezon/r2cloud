package ru.r2cloud;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.rules.TemporaryFolder;

import ru.r2cloud.util.Configuration;

public class TestConfiguration extends Configuration {

	public TestConfiguration(TemporaryFolder tempFolder) throws IOException {
		super(TestConfiguration.class.getClassLoader().getResourceAsStream("config-dev.properties"), getUserSettingsLocation(tempFolder), "config-common-test.properties", FileSystems.getDefault());
	}

	public TestConfiguration(TemporaryFolder tempFolder, FileSystem fs) throws IOException {
		super(TestConfiguration.class.getClassLoader().getResourceAsStream("config-dev.properties"), getUserSettingsLocation(tempFolder), "config-common-test.properties", fs);
	}

	public static String getUserSettingsLocation(TemporaryFolder tempFolder) {
		return tempFolder.getRoot().getAbsolutePath() + File.separator + "user.properties";
	}

}

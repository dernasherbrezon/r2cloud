package ru.r2cloud;

import java.io.File;
import java.io.IOException;

import org.junit.rules.TemporaryFolder;

import ru.r2cloud.util.Configuration;

public class TestConfiguration extends Configuration {

	public TestConfiguration(TemporaryFolder tempFolder) throws IOException {
		super(TestConfiguration.class.getClassLoader().getResourceAsStream("config-dev.properties"), tempFolder.getRoot().getAbsolutePath() + File.separator + "user.properties");
	}

}

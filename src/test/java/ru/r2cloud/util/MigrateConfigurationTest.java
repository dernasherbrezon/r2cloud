package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Properties;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.it.util.BaseTest;

public class MigrateConfigurationTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testRtlSdrConfigurationMigration() throws Exception {
		assertMigration("expected.pre.r2cloud.properties", "pre.r2cloud.properties");
	}

	private void assertMigration(String expectedProperties, String preMigrationProperties) throws Exception {
		File userSettingsLocation = new File(tempFolder.getRoot(), ".r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = MigrateConfigurationTest.class.getClassLoader().getResourceAsStream("migration/" + preMigrationProperties); FileOutputStream fos = new FileOutputStream(userSettingsLocation)) {
			Properties props = new Properties();
			props.load(is);
			props.store(fos, "");
		}
		Configuration config;
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		MigrateConfiguration migrateConfiguration = new MigrateConfiguration(config);
		migrateConfiguration.migrate();
		assertPropertiesEquals("migration/" + expectedProperties, userSettingsLocation);
	}

	private static void assertPropertiesEquals(String expected, File actual) throws Exception {
		Properties expectedProps = new Properties();
		try (InputStream is = MigrateConfigurationTest.class.getClassLoader().getResourceAsStream(expected)) {
			expectedProps.load(is);
		}
		Properties actualProps = new Properties();
		try (InputStream is = new BufferedInputStream(new FileInputStream(actual))) {
			actualProps.load(is);
		}
		for (Object curKey : expectedProps.keySet()) {
			assertTrue(actualProps.containsKey(curKey));
			assertEquals(expectedProps.get(curKey), actualProps.get(curKey));
		}
	}

}

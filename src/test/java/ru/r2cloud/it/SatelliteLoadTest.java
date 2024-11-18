package ru.r2cloud.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class SatelliteLoadTest extends RegisteredTest {

	@Test
	public void testLoad() throws Exception {
		Path p = config.getPathFromProperty("satellites.meta.location");
		Files.setLastModifiedTime(p, FileTime.from(1719525695573L, TimeUnit.MILLISECONDS));
		TestUtil.assertJson("expected/satellite.json", client.getSatellite("25338"));
	}

}

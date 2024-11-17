package ru.r2cloud.it;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class SatelliteLoadTest extends RegisteredTest {

	@Test
	public void testLoad() {
		TestUtil.assertJson("expected/satellite.json", client.getSatellite("25338"));
	}

}

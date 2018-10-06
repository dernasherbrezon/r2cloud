package ru.r2cloud.it;

import org.junit.Test;

import ru.r2cloud.it.util.BaseIT;

public class SetupIT extends BaseIT {

	@Test
	public void testSetup() {
		client.setup("ittests", "info@r2cloud.ru", "1");
	}

}

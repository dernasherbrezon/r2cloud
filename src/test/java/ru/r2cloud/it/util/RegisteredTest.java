package ru.r2cloud.it.util;

import org.junit.Before;

public abstract class RegisteredTest extends BaseTest {
	
	@Before
	@Override
	public void start() throws Exception {
		super.start();
		client.setup("ittests", "info@r2cloud.ru", "1");
		client.login("info@r2cloud.ru", "1");
	}

}

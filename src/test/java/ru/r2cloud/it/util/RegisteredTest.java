package ru.r2cloud.it.util;

import org.junit.Before;

public abstract class RegisteredTest extends BaseTest {
	
	@Before
	@Override
	public void start() throws Exception {
		super.start();
		client.setup("ittests", username, password);
		client.login(username, password);
	}

}

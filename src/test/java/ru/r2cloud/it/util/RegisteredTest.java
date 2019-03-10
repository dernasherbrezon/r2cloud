package ru.r2cloud.it.util;

import org.junit.Before;

public abstract class RegisteredTest extends BaseTest {
	
	protected String username = "info@r2cloud.ru";
	protected String password = "1";

	@Before
	@Override
	public void start() throws Exception {
		super.start();
		client.setup("ittests", username, password);
		client.login(username, password);
	}

}

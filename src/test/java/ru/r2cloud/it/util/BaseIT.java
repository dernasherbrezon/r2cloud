package ru.r2cloud.it.util;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

public abstract class BaseIT {

	protected RestClient client;

	@Before
	public void start() throws Exception {
		client = new RestClient(System.getProperty("r2cloud.baseurl"));
	}

	@After
	public void stop() throws IOException {
		if (client != null) {
			client.close();
		}
	}
}

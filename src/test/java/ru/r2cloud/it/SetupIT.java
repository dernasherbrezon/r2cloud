package ru.r2cloud.it;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SetupIT {

	private RestClient client;

	@Test
	public void setup() {
		client.setup("ittests", "info@r2cloud.ru", "1");
	}

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

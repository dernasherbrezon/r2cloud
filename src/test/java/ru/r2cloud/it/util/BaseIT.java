package ru.r2cloud.it.util;

import org.junit.Before;

public abstract class BaseIT {

	protected RestClient client;

	@Before
	public void start() throws Exception {
		client = new RestClient(System.getProperty("r2cloud.baseurl"));
	}
	
}

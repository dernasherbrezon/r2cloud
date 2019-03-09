package ru.r2cloud.it.util;

import static org.junit.Assert.assertNotNull;

import java.net.http.HttpResponse;

import org.junit.Before;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public abstract class BaseTest {

	protected RestClient client;

	@Before
	public void start() throws Exception {
		client = new RestClient(System.getProperty("r2cloud.baseurl"));
	}

	public static void assertErrorInField(String field, HttpResponse<String> response) {
		JsonObject result = (JsonObject) Json.parse(response.body());
		JsonObject errors = (JsonObject) result.get("errors");
		assertNotNull(errors);
		assertNotNull(errors.get(field));
	}

}

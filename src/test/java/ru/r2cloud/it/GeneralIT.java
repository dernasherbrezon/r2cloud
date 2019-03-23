package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class GeneralIT extends RegisteredTest {

	@Test
	public void testConfiguration() {
		double lat = 10.1;
		double lng = 23.4;
		boolean autoUpdate = true;
		client.setGeneralConfiguration(lat, lng, autoUpdate);
		JsonObject configuration = client.getGeneralConfiguration();
		assertEquals(lat, configuration.getDouble("lat", 0.0), 0.0);
		assertEquals(lng, configuration.getDouble("lng", 0.0), 0.0);
		assertEquals(autoUpdate, configuration.getBoolean("autoUpdate", !autoUpdate));
	}
	
	@Test
	public void testInvalidLat() {
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(null, 10.1, true);
		assertEquals(400, response.statusCode());
		assertErrorInField("lat", response);
	}

	@Test
	public void testInvalidLng() {
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(10.1, null, true);
		assertEquals(400, response.statusCode());
		assertErrorInField("lng", response);
	}
}

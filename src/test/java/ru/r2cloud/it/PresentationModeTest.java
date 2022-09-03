package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.http.HttpResponse;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.BaseTest;

public class PresentationModeTest extends BaseTest {

	@Test
	public void testAuthFailureIfPresentationModeDisabled() {
		HttpResponse<String> response = client.getDataWithResponse("/api/v1/presentationMode");
		assertEquals(401, response.statusCode());
	}

	@Test
	public void testSucces() throws Exception {
		File basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + "41460" + File.separator + "data" + File.separator + "1559942730784");
		TestUtil.copy("aausat4Observation/1559942730784.json", new File(basepath, "meta.json"));
		TestUtil.copy("aausat4Observation/data.bin", new File(basepath, "data.bin"));
		config.setProperty("presentationMode", true);
		// force cache reload
		client.getObservationPresentation("41460", "1559942730784");
		
		JsonObject data = client.getPresentationModeData();
		JsonArray observations = data.get("observations").asArray();
		assertNotNull(observations);
		assertEquals(6, observations.size());
		JsonObject baseStation = data.get("basestation").asObject();
		assertNotNull(baseStation);
	}

}

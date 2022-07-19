package ru.r2cloud.it;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.util.Configuration;

public class SharedRotatorTest extends RegisteredTest {

	@Test
	public void testSuccess() {
		JsonArray schedule = client.getFullSchedule();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		for (int i = 0; i < schedule.size(); i++) {
			JsonObject obj = schedule.get(i).asObject();
			obj.add("startFormatted", sdf.format(new Date(obj.get("start").asLong())));
			obj.add("endFormatted", sdf.format(new Date(obj.get("end").asLong())));
		}
		TestUtil.assertJson("expected/sharedRotatorSchedule.json", schedule);
	}

	@Override
	protected Configuration prepareConfiguration() throws IOException {
		Configuration result = super.prepareConfiguration();
		result.setProperty("satellites.meta.location", "satellites-test-schedule.json");
		result.setProperty("sdr.device.0.rotator.enabled", true);
		result.setProperty("sdr.device.0.rotctrld.hostname", "127.0.0.1");
		result.setProperty("sdr.device.0.rotctrld.port", BaseTest.ROTCTRLD_PORT);
		result.setProperty("sdr.device.0.minFrequency", 433000000);
		result.setProperty("sdr.device.0.maxFrequency", 480000000);

		result.setProperty("sdr.device.1.rotator.enabled", true);
		result.setProperty("sdr.device.1.rotctrld.hostname", "127.0.0.1");
		result.setProperty("sdr.device.1.rotctrld.port", BaseTest.ROTCTRLD_PORT);
		result.setProperty("sdr.device.1.minFrequency", 433000000);
		result.setProperty("sdr.device.1.maxFrequency", 480000000);

		result.remove("r2lora.devices");
		result.remove("loraat.devices");

		return result;
	}
}

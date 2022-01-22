package ru.r2cloud.it;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class OverviewTest extends RegisteredTest {

	@Test
	public void testSuccess() {
		JsonObject overview = client.getOverview();
		// time is unstable
		overview.remove("serverTime");
		TestUtil.assertJson("expectedOverview.json", overview);
	}
}

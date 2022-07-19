package ru.r2cloud.it;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleListTest extends RegisteredTest {

	@Test
	public void testList() {
		JsonArray schedule = client.getSchedule();
		TestUtil.assertJson("expected/schedule.json", schedule);
	}

}

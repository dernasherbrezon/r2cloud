package ru.r2cloud.it;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;

import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleListTest extends RegisteredTest {

	@Test
	public void testList() {
		JsonArray schedule = client.getSchedule();
		assertTrue(schedule.size() > 0);
	}

}

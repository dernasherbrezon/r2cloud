package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;

import org.junit.Test;

public class TimetableTest {

	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");

	@Test
	public void testFully() throws Exception {
		Timetable table = new Timetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10")));
		assertTrue(table.addFully(create("12:10", "12:20")));
		assertFalse(table.addFully(create("12:15", "12:25")));
		assertTrue(table.addFully(create("12:20", "12:30")));
		assertTrue(table.addFully(create("11:50", "12:00")));
		assertTrue(table.addFully(create("12:40", "12:50")));
		assertTrue(table.addFully(create("12:30", "12:40")));
	}

	@Test
	public void testPartially() throws Exception {
		Timetable table = new Timetable(120_000);
		assertSlot("12:00", "12:10", table.addPatially(create("12:00", "12:10")));
		assertNull(table.addPatially(create("11:59", "12:09")));
		assertSlot("11:52", "12:00", table.addPatially(create("11:52", "12:02")));
		
		// partial fit with tolerances on both ends
		assertSlot("12:10", "12:18", table.addPatially(create("12:08", "12:18")));
		assertSlot("12:20", "12:30", table.addPatially(create("12:20", "12:30")));
		assertSlot("12:18", "12:20", table.addPatially(create("12:16", "12:22")));
		// exact fit in the middle
		assertSlot("12:40", "12:50", table.addPatially(create("12:40", "12:50")));
		assertSlot("12:30", "12:40", table.addPatially(create("12:30", "12:40")));
		
		// partial fit at the start
		assertSlot("13:00", "13:10", table.addPatially(create("13:00", "13:10")));
		assertSlot("12:50", "12:58", table.addPatially(create("12:48", "12:58")));
		
		// partial fit at the end
		assertSlot("13:20", "13:30", table.addPatially(create("13:20", "13:30")));
		assertSlot("13:12", "13:20", table.addPatially(create("13:12", "13:22")));
	}

	private static void assertSlot(String start, String end, TimeSlot actual) throws Exception {
		assertNotNull(actual);
		assertEquals(SDF.parse(start).getTime(), actual.getStart());
		assertEquals(SDF.parse(end).getTime(), actual.getEnd());
	}

	private static TimeSlot create(String start, String end) throws Exception {
		TimeSlot result = new TimeSlot();
		synchronized (SDF) {
			result.setStart(SDF.parse(start).getTime());
			result.setEnd(SDF.parse(end).getTime());
		}
		return result;
	}
}

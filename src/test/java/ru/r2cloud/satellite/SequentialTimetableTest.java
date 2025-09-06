package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

public class SequentialTimetableTest {

	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm", Locale.US);
	
	@Test
	public void testClear() throws Exception {
		SequentialTimetable table = new SequentialTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10")));
		table.clear();
		assertTrue(table.addFully(create("12:00", "12:10")));
	}

	@Test
	public void testRemove() throws Exception {
		SequentialTimetable table = new SequentialTimetable(60_000);
		// remove unknown slot
		assertFalse(table.remove(create("12:00", "12:10")));
		// remove null slot
		assertFalse(table.remove(null));
		
		assertTrue(table.addFully(create("12:00", "12:10")));
		assertTrue(table.remove(create("12:00", "12:10")));
	}

	@Test
	public void testFully() throws Exception {
		SequentialTimetable table = new SequentialTimetable(60_000);
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
		SequentialTimetable table = new SequentialTimetable(120_000);
		assertSlot("12:00", "12:10", table.addPartially(create("12:00", "12:10")));
		assertNull(table.addPartially(create("11:59", "12:09")));
		assertSlot("11:52", "12:00", table.addPartially(create("11:52", "12:02")));

		// partial fit with tolerances on both ends
		assertSlot("12:10", "12:18", table.addPartially(create("12:08", "12:18")));
		assertSlot("12:20", "12:30", table.addPartially(create("12:20", "12:30")));
		assertSlot("12:18", "12:20", table.addPartially(create("12:16", "12:22")));
		// exact fit in the middle
		assertSlot("12:40", "12:50", table.addPartially(create("12:40", "12:50")));
		assertSlot("12:30", "12:40", table.addPartially(create("12:30", "12:40")));

		// partial fit at the start
		assertSlot("13:00", "13:10", table.addPartially(create("13:00", "13:10")));
		assertSlot("12:50", "12:58", table.addPartially(create("12:48", "12:58")));

		// partial fit at the end
		assertSlot("13:20", "13:30", table.addPartially(create("13:20", "13:30")));
		assertSlot("13:12", "13:20", table.addPartially(create("13:12", "13:22")));
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

package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;

import org.junit.Test;

public class OverlappedTimetableTest {

	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");

	@Test
	public void testGrowBand() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		assertTrue(table.addFully(create("12:05", "12:15", 1)));
		assertTrue(table.addFully(create("12:09", "12:19", 1)));
		assertTrue(table.addFully(create("11:55", "12:05", 1)));
	}

	@Test
	public void testMergeBands() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		assertTrue(table.addFully(create("12:15", "12:25", 1)));
		assertTrue(table.addFully(create("12:08", "12:18", 1)));
	}

	@Test
	public void testAddPartial() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(120_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		TimeSlot outOfBand = create("11:55", "12:05", 2);
		assertFalse(table.addFully(outOfBand));
		assertSlot("11:55", "12:00", 2, table.addPartially(outOfBand));
		assertNull(table.addPartially(create("11:59", "12:10", 2)));
		outOfBand = create("12:08", "12:15", 3);
		assertFalse(table.addFully(outOfBand));
		assertSlot("12:10", "12:15", 3, table.addPartially(outOfBand));
	}

	@Test
	public void testAddPartialBetweenExisting1() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		assertTrue(table.addFully(create("12:20", "12:30", 2)));
		assertSlot("12:08", "12:20", 1, table.addPartially(create("12:08", "12:22", 1)));
	}

	@Test
	public void testAddPartialBetweenExisting2() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		assertTrue(table.addFully(create("12:20", "12:30", 2)));
		assertSlot("12:10", "12:22", 2, table.addPartially(create("12:08", "12:22", 2)));
	}

	@Test
	public void testAddPartialBetweenExisting3() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		assertTrue(table.addFully(create("12:00", "12:10", 1)));
		assertTrue(table.addFully(create("12:20", "12:30", 2)));
		assertSlot("12:10", "12:20", 3, table.addPartially(create("12:08", "12:22", 3)));
	}
	
	@Test
	public void testRemoveLastSlot() throws Exception {
		OverlappedTimetable table = new OverlappedTimetable(60_000);
		TimeSlot slot1 = create("12:00", "12:10", 1);
		assertTrue(table.addFully(slot1));
		assertTrue(table.remove(slot1));
		TimeSlot slot2 = create("12:00", "12:10", 2);
		assertTrue(table.addFully(slot2));
	}
	
	private static void assertSlot(String start, String end, long freq, TimeSlot actual) throws Exception {
		assertNotNull(actual);
		assertEquals(SDF.parse(start).getTime(), actual.getStart());
		assertEquals(SDF.parse(end).getTime(), actual.getEnd());
		assertEquals(freq, actual.getFrequency());
	}

	private static TimeSlot create(String start, String end, long freq) throws Exception {
		TimeSlot result = new TimeSlot();
		synchronized (SDF) {
			result.setStart(SDF.parse(start).getTime());
			result.setEnd(SDF.parse(end).getTime());
			result.setFrequency(freq);
		}
		return result;
	}
}

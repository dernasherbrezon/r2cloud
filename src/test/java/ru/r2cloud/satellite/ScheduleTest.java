package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.DefaultScheduleEntry;

public class ScheduleTest {

	private Schedule<DefaultScheduleEntry> schedule;

	@Test
	public void testAddAndGet() {
		DefaultScheduleEntry entry = create();
		schedule.add(entry);
		assertEquals(entry, schedule.get(entry.getId()));
	}

	@Test
	public void testAddingNewWillCancelPrevious() {
		DefaultScheduleEntry entry1 = create();
		DefaultScheduleEntry entry2 = create();
		entry2.setId(entry1.getId());
		schedule.add(entry1);
		schedule.add(entry2);
		assertTrue(entry1.isCancelled());
		assertFalse(entry2.isCancelled());
	}

	@Test
	public void testAddingTheSameIsNoOp() {
		DefaultScheduleEntry entry = create();
		schedule.add(entry);
		schedule.add(entry);
		assertFalse(entry.isCancelled());
	}

	@Test
	public void testCancelAndRemove() {
		DefaultScheduleEntry entry = create();
		schedule.add(entry);
		schedule.cancel(entry.getId());
		assertTrue(entry.isCancelled());
		assertNull(schedule.get(entry.getId()));
	}

	@Test
	public void testNullPermitted() {
		schedule.add(null);
		// no exception
		assertTrue(true);
	}

	@Test
	public void testCancelNullPermitted() {
		schedule.cancel(null);
		// no exception
		assertTrue(true);
	}

	@Test
	public void testGetNullPermitted() {
		assertNull(schedule.get(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInput() {
		DefaultScheduleEntry entry = create();
		entry.setId(null);
		schedule.add(entry);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInput2() {
		DefaultScheduleEntry entry = create();
		entry.setEndTimeMillis(0);
		entry.setStartTimeMillis(1);
		schedule.add(entry);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInput3() {
		schedule.getOverlap(1, 0);
	}

	@Test
	public void testPartialOverlap() {
		DefaultScheduleEntry entry = create();
		entry.setStartTimeMillis(0);
		entry.setEndTimeMillis(2);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(1, 3));
	}

	@Test
	public void testPartialOverlap2() {
		DefaultScheduleEntry entry = create();
		entry.setStartTimeMillis(1);
		entry.setEndTimeMillis(3);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(0, 2));
	}

	@Test
	public void testFullOverlap() {
		DefaultScheduleEntry entry = create();
		entry.setStartTimeMillis(0);
		entry.setEndTimeMillis(4);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(1, 3));
	}
	
	@Test
	public void testFullOverlap2() {
		DefaultScheduleEntry entry = create();
		entry.setStartTimeMillis(1);
		entry.setEndTimeMillis(3);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(0, 4));
	}

	private static DefaultScheduleEntry create() {
		DefaultScheduleEntry entry = new DefaultScheduleEntry();
		entry.setStartTimeMillis(0);
		entry.setEndTimeMillis(1);
		entry.setId(UUID.randomUUID().toString());
		return entry;
	}

	@Before
	public void start() {
		schedule = new Schedule<>();
	}

}

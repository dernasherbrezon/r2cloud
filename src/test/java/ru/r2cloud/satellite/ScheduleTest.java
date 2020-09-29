package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.model.ObservationRequest;

public class ScheduleTest {

	private Schedule schedule;

	@Test
	public void testAddAndGet() {
		ScheduledObservation entry = create();
		schedule.add(entry);
		assertEquals(entry, schedule.get(entry.getId()));
	}

	// @Test
	// public void testAddingNewWillCancelPrevious() {
	// ScheduledObservation entry1 = create();
	// ScheduledObservation entry2 = create();
	// entry2.setId(entry1.getId());
	// schedule.add(entry1);
	// schedule.add(entry2);
	// assertTrue(entry1.isCancelled());
	// assertFalse(entry2.isCancelled());
	// }

	@Test
	public void testAddingTheSameIsNoOp() {
		ScheduledObservation entry = create();
		schedule.add(entry);
		schedule.add(entry);
		assertFalse(entry.isCancelled());
	}

	@Test
	public void testCancelAndRemove() {
		ScheduledObservation entry = create();
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
	public void testValidateInput2() {
		ScheduledObservation entry = create(1, 0);
		schedule.add(entry);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInput3() {
		schedule.getOverlap(1, 0);
	}

	@Test
	public void testPartialOverlap() {
		ScheduledObservation entry = create(0, 2);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(1, 3));
	}

	@Test
	public void testPartialOverlap2() {
		ScheduledObservation entry = create(1, 3);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(0, 2));
	}

	@Test
	public void testFullOverlap() {
		ScheduledObservation entry = create(0, 4);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(1, 3));
	}

	@Test
	public void testFullOverlap2() {
		ScheduledObservation entry = create(1, 3);
		schedule.add(entry);
		assertNotNull(schedule.getOverlap(0, 4));
	}

	private static ScheduledObservation create() {
		return create(0, 1);
	}

	private static ScheduledObservation create(long start, long end) {
		ObservationRequest req = new ObservationRequest();
		req.setStartTimeMillis(start);
		req.setEndTimeMillis(end);
		req.setId(UUID.randomUUID().toString());
		req.setSatelliteId(UUID.randomUUID().toString());
		ScheduledObservation entry = new ScheduledObservation(req, null, null, null, null);
		return entry;
	}

	@Before
	public void start() {
		schedule = new Schedule(null);
	}

}

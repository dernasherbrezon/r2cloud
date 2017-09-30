package ru.r2cloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RtlSdrLockTest {

	@Test
	public void testBasic() {
		RtlSdrLock lock = new RtlSdrLock();
		lock.register(RtlSdrListener1.class, 1);
		lock.register(RtlSdrListener2.class, 2);

		RtlSdrListener1 listener1 = new RtlSdrListener1();
		RtlSdrListener2 listener2 = new RtlSdrListener2();

		assertTrue(lock.tryLock(listener1));
		assertTrue(lock.tryLock(listener2));
		assertTrue(listener1.isSuspended());
		lock.release(listener2);
		assertTrue(listener1.isResumed());
	}

	@Test
	public void testSingleListener() {
		RtlSdrLock lock = new RtlSdrLock();
		lock.register(RtlSdrListener1.class, 1);

		RtlSdrListener1 listener1 = new RtlSdrListener1();

		assertTrue(lock.tryLock(listener1));
		lock.release(listener1);
		assertFalse(listener1.isResumed());
	}

	@Test
	public void testPriority() {
		RtlSdrLock lock = new RtlSdrLock();
		lock.register(RtlSdrListener1.class, 1);
		lock.register(RtlSdrListener2.class, 2);

		RtlSdrListener1 listener1 = new RtlSdrListener1();
		RtlSdrListener2 listener2 = new RtlSdrListener2();

		assertTrue(lock.tryLock(listener2));
		assertFalse(lock.tryLock(listener1));
		lock.release(listener2);
		assertTrue(listener1.isResumed());
	}

}

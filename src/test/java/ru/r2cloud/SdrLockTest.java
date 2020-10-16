package ru.r2cloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.r2cloud.sdr.SdrLock;

public class SdrLockTest {

	@Test
	public void testLockByUnknown() {
		SdrLock lock = new SdrLock();
		lock.register(RtlSdrListener1.class, 1);
		
		assertFalse(lock.tryLock(new RtlSdrListener2()));
	}
	
	@Test
	public void testBasic() {
		SdrLock lock = new SdrLock();
		lock.register(RtlSdrListener1.class, 1);
		lock.register(RtlSdrListener2.class, 2);

		RtlSdrListener1 listener1 = new RtlSdrListener1();
		RtlSdrListener2 listener2 = new RtlSdrListener2();

		assertTrue(lock.tryLock(listener1));
		assertTrue(lock.tryLock(listener2));
		assertTrue(listener1.isSuspended());
		lock.unlock(listener2);
		assertTrue(listener1.isResumed());
	}

	@Test
	public void testSingleListener() {
		SdrLock lock = new SdrLock();
		lock.register(RtlSdrListener1.class, 1);

		RtlSdrListener1 listener1 = new RtlSdrListener1();

		assertTrue(lock.tryLock(listener1));
		lock.unlock(listener1);
		assertFalse(listener1.isResumed());
	}

	@Test
	public void testPriority() {
		SdrLock lock = new SdrLock();
		lock.register(RtlSdrListener1.class, 1);
		lock.register(RtlSdrListener2.class, 2);

		RtlSdrListener1 listener1 = new RtlSdrListener1();
		RtlSdrListener2 listener2 = new RtlSdrListener2();

		assertTrue(lock.tryLock(listener2));
		assertFalse(lock.tryLock(listener1));
		lock.unlock(listener2);
		assertTrue(listener1.isResumed());
	}

}

package ru.r2cloud.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamingThreadFactory implements ThreadFactory {

	private final String prefix;
	private final AtomicInteger threadCreated = new AtomicInteger(0);

	public NamingThreadFactory(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, prefix + "-" + threadCreated.incrementAndGet());
	}

}

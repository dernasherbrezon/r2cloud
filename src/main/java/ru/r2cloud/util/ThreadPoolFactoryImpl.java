package ru.r2cloud.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPoolFactoryImpl implements ThreadPoolFactory {

	@Override
	public ScheduledExecutorService newScheduledThreadPool(int i, NamingThreadFactory namingThreadFactory) {
		return Executors.newScheduledThreadPool(i, namingThreadFactory);
	}

}

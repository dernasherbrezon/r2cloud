package ru.r2cloud.util;

import java.util.concurrent.ScheduledExecutorService;

public interface ThreadPoolFactory {

	ScheduledExecutorService newScheduledThreadPool(int i, NamingThreadFactory namingThreadFactory);

}

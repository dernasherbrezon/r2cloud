package ru.r2cloud.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public interface ProcessWrapper {

	int waitFor() throws InterruptedException;

	boolean isAlive();

	void destroy();

	boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;

	ProcessWrapper destroyForcibly();

	OutputStream getOutputStream();

	InputStream getInputStream();
	
	InputStream getErrorStream();

}
package ru.r2cloud;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class ExecuteNowThreadFactory implements ThreadPoolFactory, ScheduledExecutorService {

	@Override
	public ScheduledExecutorService newScheduledThreadPool(int i, NamingThreadFactory namingThreadFactory) {
		return this;
	}

	@Override
	public void shutdown() {
		// do nothing
	}

	@Override
	public List<Runnable> shutdownNow() {
		// do nothing
		return null;
	}

	@Override
	public boolean isShutdown() {
		// do nothing
		return false;
	}

	@Override
	public boolean isTerminated() {
		// do nothing
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return true;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		// do nothing
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		// do nothing
		return null;
	}

	@Override
	public Future<?> submit(Runnable task) {
		// do nothing
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		// do nothing
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		// do nothing
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		// do nothing
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		// do nothing
		return null;
	}

	@Override
	public void execute(Runnable command) {
		// do nothing
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		// do nothing
		return null;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		// do nothing
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		command.run();
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		// do nothing
		return null;
	}

}

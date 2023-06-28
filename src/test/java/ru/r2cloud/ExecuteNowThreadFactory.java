package ru.r2cloud;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class ExecuteNowThreadFactory implements ThreadPoolFactory, ScheduledExecutorService {

	private final boolean sameThread;
	private ScheduledExecutorService impl;

	public ExecuteNowThreadFactory(boolean sameThread) {
		this.sameThread = sameThread;
	}

	public ExecuteNowThreadFactory() {
		this(true);
	}

	@Override
	public long getThreadPoolShutdownMillis() {
		return 0;
	}

	@Override
	public ScheduledExecutorService newScheduledThreadPool(int i, NamingThreadFactory namingThreadFactory) {
		if (!sameThread) {
			impl = Executors.newScheduledThreadPool(i, namingThreadFactory);
		}
		return this;
	}

	@Override
	public void shutdown() {
		// do nothing
	}

	@Override
	public List<Runnable> shutdownNow() {
		if (impl != null) {
			return impl.shutdownNow();
		}
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
		if (impl != null) {
			return impl.awaitTermination(timeout, unit);
		}
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
		if (sameThread) {
			command.run();
		} else {
			impl.execute(command);
		}
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
		if (impl != null) {
			ScheduledFuture<?> future = impl.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					command.run();
				}
			}, 0, period, unit);
			return future;
		}
		command.run();
		return new NoOpScheduledFuture<>();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		// do nothing
		return null;
	}

}

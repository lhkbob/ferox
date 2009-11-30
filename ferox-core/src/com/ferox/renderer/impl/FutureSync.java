package com.ferox.renderer.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureSync<V> implements Future<V> {
	private final Sync<V> sync;
	
	public FutureSync(Sync<V> sync) {
		if (sync == null)
			throw new NullPointerException("Cannot create a FutureSync with a null Sync");
		this.sync = sync;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return sync.cancel(mayInterruptIfRunning);
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return sync.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return sync.get(timeout, unit);
	}

	@Override
	public boolean isCancelled() {
		return sync.isCancelled();
	}

	@Override
	public boolean isDone() {
		return sync.isDone();
	}
}

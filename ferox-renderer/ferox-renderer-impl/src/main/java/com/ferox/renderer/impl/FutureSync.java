package com.ferox.renderer.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FutureSync is a Future implementation that relies on a {@link Sync} to manage
 * the Future's state and provide the final value to return from {@link #get()}.
 * 
 * @author Michael Ludwig
 * 
 * @param <V>
 */
public class FutureSync<V> implements Future<V> {
    private final Sync<V> sync;

    /**
     * Construct a new FutureSync that wraps the given Sync.
     * 
     * @param sync The Sync that's wrapped and will provide the necessary
     *            synchronization logic
     * @throws NullPointerException if sync is null
     */
    public FutureSync(Sync<V> sync) {
        if (sync == null) {
            throw new NullPointerException("Cannot create a FutureSync with a null Sync");
        }
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

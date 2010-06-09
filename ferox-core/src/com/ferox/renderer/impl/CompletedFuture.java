package com.ferox.renderer.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CompletedFuture is a Future implementation that represents an instantaneous
 * action. It is constructed with the value to return from {@link #get()}, so
 * it is always considered completed and cannot be cancelled.
 * @author Michael Ludwig
 *
 * @param <V>
 */
public class CompletedFuture<V> implements Future<V> {
    private final V value;

    /**
     * Create a new CompletedFuture that returns the given <tt>value</tt> from
     * {@link #get()}.
     * 
     * @param value This Future's completed value
     */
    public CompletedFuture(V value) {
        this.value = value;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }
}

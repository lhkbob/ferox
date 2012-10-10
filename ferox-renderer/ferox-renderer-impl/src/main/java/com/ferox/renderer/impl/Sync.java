/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 
 */
package com.ferox.renderer.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * <p>
 * Sync is an AQS implementation that wraps a Callable and provides the
 * necessary logic needed for a Future to monitor the Sync and wait for the
 * Callable to complete. The Sync provides a {@link #run()} that executes the
 * Callable on the calling Thread. Additionally, it exposes a
 * {@link #set(Object)} method to manually override the result to return, which
 * can be useful in some situations where you don't want to cancel the task but
 * don't want to run the actual Callable.
 * </p>
 * <p>
 * This implementation was extracted from the java.util.concurrent package,
 * where it was originally an inner class within FutureTask.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <V>
 */
public class Sync<V> extends AbstractQueuedSynchronizer implements Runnable {
    private static final long serialVersionUID = 1L;

    private static final int RUNNING = 1;
    private static final int COMPLETED = 2;
    private static final int CANCELLED = 8;

    private final Callable<V> task;
    private volatile Thread runner;

    private V result;
    private Throwable exception;

    public Sync(Callable<V> task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        this.task = task;
    }

    public Callable<V> getTask() {
        return task;
    }

    public boolean isCancelled() {
        return getState() == CANCELLED;
    }

    public boolean isDone() {
        return completed(getState());
    }

    public boolean cancel(boolean mayInterrupt) {
        for (;;) {
            int s = getState();
            if (completed(s)) {
                return false;
            }
            if (compareAndSetState(s, CANCELLED)) {
                break;
            }
        }

        if (mayInterrupt) {
            Thread r = runner;
            if (r != null) {
                r.interrupt();
            }
        }
        releaseShared(0);
        return true;
    }

    public V get() throws InterruptedException, ExecutionException {
        acquireSharedInterruptibly(0);
        if (getState() == CANCELLED) {
            throw new CancellationException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    public V get(long timeout, TimeUnit units) throws InterruptedException, ExecutionException, TimeoutException {
        if (!tryAcquireSharedNanos(0, units.toNanos(timeout))) {
            throw new TimeoutException();
        }
        if (getState() == CANCELLED) {
            throw new CancellationException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public void run() {
        if (!compareAndSetState(0, RUNNING)) {
            return;
        }
        try {
            runner = Thread.currentThread();
            if (getState() == RUNNING) {
                set(task.call());
            } else {
                releaseShared(0); // cancel
            }
        } catch (Throwable ex) {
            setException(ex);
        }
    }

    public void set(V v) {
        for (;;) {
            int s = getState();
            if (s == COMPLETED) {
                return;
            }
            if (s == CANCELLED) {
                // aggressively release to set runner to null,
                // in case we are racing with a cancel request
                // that will try to interrupt runner
                releaseShared(0);
                return;
            }
            if (compareAndSetState(s, COMPLETED)) {
                result = v;
                releaseShared(0);
                return;
            }
        }
    }

    private void setException(Throwable t) {
        for (;;) {
            int s = getState();
            if (s == COMPLETED) {
                return;
            }
            if (s == CANCELLED) {
                // aggressively release to set runner to null,
                // in case we are racing with a cancel request
                // that will try to interrupt runner
                releaseShared(0);
                return;
            }
            if (compareAndSetState(s, COMPLETED)) {
                exception = t;
                result = null;
                releaseShared(0);
                return;
            }
        }
    }

    private boolean completed(int state) {
        return (state & (COMPLETED | CANCELLED)) != 0;
    }

    /**
     * Implements AQS base acquire to succeed if ran or cancelled
     */
    @Override
    protected int tryAcquireShared(int ignore) {
        return isDone() ? 1 : -1;
    }

    /**
     * Implements AQS base release to always signal after setting final done
     * status by nulling runner thread.
     */
    @Override
    protected boolean tryReleaseShared(int ignore) {
        runner = null;
        return true;
    }
}
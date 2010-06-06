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

// TODO: document, and give credit to FutureTask.Sync
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
		if (task == null)
			throw new NullPointerException("Task cannot be null");
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
			if (completed(s))
				return false;
			if (compareAndSetState(s, CANCELLED))
				break;
		}
		
		if (mayInterrupt) {
			Thread r = runner;
			if (r != null)
				r.interrupt();
		}
		releaseShared(0);
		return true;
	}
	
	public V get() throws InterruptedException, ExecutionException {
		acquireSharedInterruptibly(0);
		if (getState() == CANCELLED)
			throw new CancellationException();
		if (exception != null)
			throw new ExecutionException(exception);
		return result;
	}
	
	public V get(long nanos, TimeUnit units) throws InterruptedException, ExecutionException, TimeoutException {
		if (!tryAcquireSharedNanos(0, nanos))
			throw new TimeoutException();
		if (getState() == CANCELLED)
			throw new CancellationException();
		if (exception != null)
			throw new ExecutionException(exception);
		return result;
	}
	
	@Override
	public void run() {
		 if (!compareAndSetState(0, RUNNING))
             return;
         try {
             runner = Thread.currentThread();
             if (getState() == RUNNING) // recheck after setting thread
                 set(task.call());
             else
                 releaseShared(0); // cancel
         } catch (Throwable ex) {
             setException(ex);
         }
	}
	
	public void set(V v) {
		for (;;) {
			int s = getState();
			if (s == COMPLETED)
				return;
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
			if (s == COMPLETED)
				return;
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
        return isDone()? 1 : -1;
    }

    /**
     * Implements AQS base release to always signal after setting
     * final done status by nulling runner thread.
     */
	@Override
    protected boolean tryReleaseShared(int ignore) {
        runner = null;
        return true;
    }
}
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
package com.ferox.input;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * <p/>
 * MouseKeyEventDispatcher is a convenience class that implements the necessary logic to
 * register and unregister listeners as required by the {@link MouseEventSource} and
 * {@link KeyEventSource} interfaces, and dispatch events to those listeners.
 * <p/>
 * Event sources can create a MouseKeyEventDispatcher and delegate their interface methods
 * to the dispatcher as appropriate. When their low-level event system or polling produces
 * an event, they can then invoke {@link #dispatchEvent(Event)} to have their dispatcher
 * invoke all listeners on an internal thread.
 * <p/>
 * When an event source is destroyed, they must be sure to call {@link #shutdown()} to end
 * the internal dispatch thread.
 * <p/>
 * This class is thread safe.
 *
 * @author Michael Ludwig
 */
public class MouseKeyEventDispatcher {
    private final ExecutorService executor;
    private final CopyOnWriteArrayList<KeyListener> keyListeners;
    private final CopyOnWriteArrayList<MouseListener> mouseListeners;

    private final MouseKeyEventSource source;

    /**
     * Create a new MouseKeyEventDispatcher.
     *
     * @param source The event source that produced events dispatched by this dispatcher
     */
    public MouseKeyEventDispatcher(MouseKeyEventSource source) {
        if (source == null) {
            throw new NullPointerException("Source cannot be null");
        }
        this.source = source;
        executor = Executors.newFixedThreadPool(1);
        keyListeners = new CopyOnWriteArrayList<KeyListener>();
        mouseListeners = new CopyOnWriteArrayList<MouseListener>();
    }

    /**
     * @return The source of events that are dispatched by this dispatcher
     */
    public MouseKeyEventSource getSource() {
        return source;
    }

    /**
     * Function compatible with {@link KeyEventSource#addKeyListener(KeyListener)}.
     *
     * @param listener The listener to register
     */
    public void addKeyListener(KeyListener listener) {
        if (listener == null) {
            throw new NullPointerException("KeyListener cannot be null");
        }
        keyListeners.addIfAbsent(listener);
    }

    /**
     * Function compatible with {@link KeyEventSource#removeKeyListener(KeyListener)}.
     *
     * @param listener The listener to unregister
     */
    public void removeKeyListener(KeyListener listener) {
        if (listener == null) {
            throw new NullPointerException("KeyListener cannot be null");
        }
        keyListeners.remove(listener);
    }

    /**
     * Function compatible with {@link MouseEventSource#addMouseListener(MouseListener)}.
     *
     * @param listener The listener to register
     */
    public void addMouseListener(MouseListener listener) {
        if (listener == null) {
            throw new NullPointerException("MouseListener cannot be null");
        }
        mouseListeners.addIfAbsent(listener);
    }

    /**
     * Function compatible with {@link MouseEventSource#addMouseListener(MouseListener)}.
     *
     * @param listener The listener to unregister
     */
    public void removeMouseListener(MouseListener listener) {
        if (listener == null) {
            throw new NullPointerException("MouseListener cannot be null");
        }
        mouseListeners.remove(listener);
    }

    /**
     * Dispatch the given event to all registered listeners that are interested in the
     * event. This will be invoked on an internal thread managed by this dispatcher.
     *
     * @param e The event to dispatch to listeners
     *
     * @throws IllegalArgumentException if e's source is not the same source as the
     *                                  dispatcher's
     */
    public void dispatchEvent(Event e) {
        if (e.getSource() != source) {
            throw new IllegalArgumentException(
                    "Event's source does not match this dispatcher's source");
        }

        try {
            if (e instanceof MouseEvent) {
                executor.submit(new MouseEventTask((MouseEvent) e));
            } else if (e instanceof KeyEvent) {
                executor.submit(new KeyEventTask((KeyEvent) e));
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported type of event: " + e.getClass());
            }
        } catch (RejectedExecutionException ree) {
            // ignore
        }
    }

    /**
     * Shutdown the internal thread that processes dispatched events. After this is
     * invoked, calls to {@link #dispatchEvent(Event)} will perform no action.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    private class KeyEventTask implements Runnable {
        private final KeyEvent e;

        public KeyEventTask(KeyEvent e) {
            this.e = e;
        }

        @Override
        public void run() {
            for (KeyListener l : keyListeners) {
                l.handleEvent(e);
            }
        }
    }

    private class MouseEventTask implements Runnable {
        private final MouseEvent e;

        public MouseEventTask(MouseEvent e) {
            this.e = e;
        }

        @Override
        public void run() {
            for (MouseListener l : mouseListeners) {
                l.handleEvent(e);
            }
        }
    }
}

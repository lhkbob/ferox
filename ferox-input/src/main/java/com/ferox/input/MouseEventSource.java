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

/**
 * <p/>
 * MouseEventSource is an event source for mouse events. Most often it is a window of some kind that can
 * obtain focus from the OS and receive events either through AWT, JInput, or LWJGL's native input library.
 * <p/>
 * The source is then responsible for converting those low-level events into {@link MouseEvent MouseEvents}
 * and dispatching them to registered {@link MouseListener MouseListeners}. There is no guarantee about the
 * order in which registered listeners are invoked when an event occurs.
 *
 * @author Michael Ludwig
 */
public interface MouseEventSource extends EventSource {
    /**
     * Register the given MouseListener with this MouseEventSource. Nothing is done if the given listener has
     * already been added.
     *
     * @param listener The listener to add
     *
     * @throws NullPointerException if listener is null
     */
    public void addMouseListener(MouseListener listener);

    /**
     * Remove the given MouseListener from this MouseEventSource. Nothing is done if the given listener has
     * never been added, or was already removed.
     *
     * @param listener The listener to remove
     *
     * @throws NullPointerException if listener is null
     */
    public void removeMouseListener(MouseListener listener);
}

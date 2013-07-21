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
package com.ferox.renderer;

import java.util.concurrent.Future;

/**
 * Destructibles are objects that have internal, potentially heavyweight data tied to the GPU and generally an
 * implementation dependent internal framework thread, similar to how the AWT EDT thread functions.
 * Destructibles will always be properly disposed of and cleaned up if they are garbage-collected by the JVM.
 * However, they expose a {@link #destroy()} method to give more explicit control over the timing of the
 * potentially expensive task of cleaning up the underlying GPU resources.
 *
 * @author Michael Ludwig
 */
public interface Destructible {
    /**
     * Manually trigger the destruction of the resource if it's critical the internal structures are disposed
     * of ASAP instead of waiting for the garbage collector to detect when it is weak reference-able. All
     * Destructibles will have their internal structures disposed of when they are weak reference-able but
     * this provides more control.
     * <p/>
     * This method is thread safe and can be called on the Framework's internal thread or not.  If called on
     * the Framework's internal thread the destruction will occur immediately and the returned Future will
     * already be completed.  Otherwise the disposal will be queued into the Framework's internal thread.
     * <p/>
     * Regardless of when the internal resources are actually disposed of, this instance is considered
     * destroyed immediately upon calling this method. When destroyed, all behavior besides calling destroy()
     * or {@link #isDestroyed()} is undefined.
     *
     * @return A Future to check when destruction has completed
     */
    public Future<Void> destroy();

    /**
     * @return True if {@link #destroy()} has been invoked manually
     */
    public boolean isDestroyed();
}

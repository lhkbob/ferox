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
package com.ferox.renderer.impl;

import com.ferox.renderer.Context;
import com.ferox.renderer.Surface;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractSurface is an abstract class implementing Surface. Its primary purpose is to expose additional
 * functionality needed by the components of {@link FrameworkImpl} to implement the framework system easily
 * across many adapters for OpenGL.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractSurface implements Surface {
    /**
     * @return The OpenGLContext tied to the surface, or null for surfaces that depend on others to provide
     * the context (such as FBOs)
     */
    public OpenGLContext getContext() {
        return getSurfaceDestructible().getContext();
    }

    /**
     * @return Get the managed destructible that stores the context and handles actual destruction of the
     * surface
     */
    public abstract SurfaceDestructible getSurfaceDestructible();

    /**
     * Perform actions as needed to flush this surface, as required by {@link Context#flush()}.
     *
     * @param context The current context
     */
    public abstract void flush(OpenGLContext context);

    /**
     * <p/>
     * onSurfaceActivate() is a listener method that is invoked by ContextManager when a surface is activated.
     * The provided context is the current context on the calling thread and will not be null.
     * <p/>
     * This method can be overridden by subclasses to perform more actions.
     *
     * @param context The current context
     */
    public void onSurfaceActivate(OpenGLContext context) {
    }

    @Override
    public Future<Void> destroy() {
        return getSurfaceDestructible().destroy();
    }

    @Override
    public boolean isDestroyed() {
        return getSurfaceDestructible().isDestroyed();
    }

    @Override
    public FrameworkImpl getFramework() {
        return getSurfaceDestructible().framework;
    }

    /**
     * The ManagedDestructible base class for use with Surfaces. Care should be taken so that the destructible
     * has no strong references back to its associated surface.
     */
    protected static abstract class SurfaceDestructible implements DestructibleManager.ManagedDestructible {
        private final AtomicBoolean destroyed;
        protected final FrameworkImpl framework;

        public SurfaceDestructible(FrameworkImpl framework) {
            destroyed = new AtomicBoolean(false);
            this.framework = framework;
        }

        /**
         * Return the OpenGLContext that must be current in order to render into this Surface. It can be null
         * to signal that the surface requires any other context to use (such as when a TextureSurface is
         * backed by an FBO).
         *
         * @return The context of this surface
         */
        public abstract OpenGLContext getContext();

        /**
         * Perform the actual destruction of this surface. This will only be called once and the surface's
         * lock will already be held. If the surface has a context, this method is responsible for invoking
         * {@link OpenGLContext#destroy()}.
         */
        protected abstract void destroyImpl();

        @Override
        public Future<Void> destroy() {
            // First call to destroy handles the destroy operation
            if (destroyed.compareAndSet(false, true)) {
                // Accept this even during shutdown so that surfaces are destroyed
                return framework.getContextManager().invokeOnContextThread(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // Must force a release in case this surface was a context provider
                        framework.getContextManager().forceRelease();
                        destroyImpl();
                        return null;
                    }
                }, true);
            } else {
                // If we've already been destroyed, use a completed future so
                // it's seen as completed
                return new CompletedFuture<>(null);
            }
        }

        @Override
        public boolean isDestroyed() {
            return destroyed.get();
        }
    }
}

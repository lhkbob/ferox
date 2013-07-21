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

import com.ferox.renderer.*;
import com.ferox.renderer.builder.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * FrameworkImpl is an implementation of Framework that delegates all OpenGL specific operations to a {@link
 * ResourceFactory}, {@link ContextManager} and {@link SurfaceFactory}.
 *
 * @author Michael Ludwig
 */
public class FrameworkImpl implements Framework {
    private final ManagedFramework impl;
    private Capabilities renderCaps; // final after initialize() has been called.

    // fullscreen support
    private volatile WeakReference<OnscreenSurface> fullscreenSurface;

    /**
     * <p/>
     * Create a new FrameworkImpl. After constructing an FrameworkImpl, {@link #initialize()} must be invoked
     * before it can be used.
     *
     * @param surfaceFactory  The SurfaceFactory used to create surfaces
     * @param resourceFactory The ResourceFactory that will create resource implementations for the framework
     *
     * @throws NullPointerException if surfaceFactory or resourceFactory is null
     */
    public FrameworkImpl(SurfaceFactory surfaceFactory, ResourceFactory resourceFactory) {
        if (surfaceFactory == null) {
            throw new NullPointerException("SurfaceFactory cannot be null");
        }
        if (resourceFactory == null) {
            throw new NullPointerException("ResourceFactory cannot be null");
        }

        ContextManager contextManager = new ContextManager();
        impl = new ManagedFramework(surfaceFactory, new LifeCycleManager(getClass().getSimpleName()),
                                    new DestructibleManager(), resourceFactory, contextManager);

        fullscreenSurface = null;
    }

    /**
     * Complete the initialization of the framework so that the public interface defined in {@link Framework}
     * is usable. It is recommended that concrete implementations of AbstractFramework define a static method
     * that creates the framework and invokes this method.
     */
    public void initialize() {
        impl.lifecycleManager.start(new Runnable() {
            @Override
            public void run() {
                // Start up the context manager and resource manager
                impl.contextManager.initialize(impl.lifecycleManager, impl.surfaceFactory);
                impl.destructibleManager.initialize(impl.lifecycleManager);

                // register this framework to be auto-destroyed
                impl.destructibleManager.manage(FrameworkImpl.this, impl);

                renderCaps = impl.surfaceFactory.getCapabilities();
            }
        });
    }

    @Override
    public OnscreenSurface createSurface(final OnscreenSurfaceOptions options) {
        // This task is not accepted during shutdown
        Future<OnscreenSurface> create = impl.contextManager
                                             .invokeOnContextThread(new CreateOnscreenSurface(options),
                                                                    false);
        return getFuture(create);
    }


    @Override
    public TextureSurface createSurface(final TextureSurfaceOptions options) {
        // This task is not accepted during shutdown
        Future<TextureSurface> create = impl.contextManager
                                            .invokeOnContextThread(new CreateTextureSurface(options), false);

        return getFuture(create);
    }

    @Override
    public Future<Void> destroy() {
        return impl.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return impl.isDestroyed();
    }

    @Override
    public <T> Future<T> invoke(Task<T> task) {
        // Specify false so that these tasks are only queued while the state is ACTIVE
        return impl.contextManager.invokeOnContextThread(new TaskCallable<>(task), false);
    }

    @Override
    public Future<Void> flush(final Surface surface) {
        return invoke(new Task<Void>() {
            public Void run(HardwareAccessLayer access) {
                Context context = access.setActiveSurface(surface);
                if (context != null) {
                    context.flush();
                }
                return null;
            }
        });
    }

    @Override
    public void sync() {
        getFuture(invoke(new Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                return null;
            }
        }));
    }

    @Override
    public VertexBufferBuilder newVertexBuffer() {
        return impl.resourceFactory.newVertexBufferBuilder(this);
    }

    @Override
    public ElementBufferBuilder newElementBuffer() {
        return impl.resourceFactory.newElementBufferBuilder(this);
    }

    @Override
    public ShaderBuilder newShader() {
        return impl.resourceFactory.newShaderBuilder(this);
    }

    @Override
    public Texture1DBuilder newTexture1D() {
        return impl.resourceFactory.newTexture1DBuilder(this);
    }

    @Override
    public Texture2DBuilder newTexture2D() {
        return impl.resourceFactory.newTexture2DBuilder(this);
    }

    @Override
    public TextureCubeMapBuilder newTextureCubeMap() {
        return impl.resourceFactory.newTextureCubeMapBuilder(this);
    }

    @Override
    public Texture3DBuilder newTexture3D() {
        return impl.resourceFactory.newTexture3DBuilder(this);
    }

    @Override
    public Texture1DArrayBuilder newTexture1DArray() {
        return impl.resourceFactory.newTexture1DArrayBuilder(this);
    }

    @Override
    public Texture2DArrayBuilder newTexture2DArray() {
        return impl.resourceFactory.newTexture2DArrayBuilder(this);
    }

    @Override
    public DepthMap2DBuilder newDepthMap2D() {
        return impl.resourceFactory.newDepthMap2DBuilder(this);
    }

    @Override
    public DepthCubeMapBuilder newDepthCubeMap() {
        return impl.resourceFactory.newDepthCubeMapBuilder(this);
    }

    private <T> T getFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (CancellationException e) {
            // bury the cancel request so that the help methods don't
            // throw exceptions when the framework is being destroyed
            return null;
        }
    }

    @Override
    public Capabilities getCapabilities() {
        return renderCaps;
    }

    @Override
    public DisplayMode getDefaultDisplayMode() {
        return impl.surfaceFactory.getDefaultDisplayMode();
    }

    @Override
    public OnscreenSurface getFullscreenSurface() {
        WeakReference<OnscreenSurface> f = fullscreenSurface;
        if (f != null) {
            return f.get();
        } else {
            return null;
        }
    }

    /**
     * @return The ResourceFactory that handles resource creation and refreshing
     */
    public ResourceFactory getResourceFactory() {
        return impl.resourceFactory;
    }

    /**
     * @return The ContextManager that handles threading for the contexts of all created surfaces
     */
    public ContextManager getContextManager() {
        return impl.contextManager;
    }

    /**
     * @return The LifeCycleManager that controls the Framework's lifecycle implementation
     */
    public LifeCycleManager getLifeCycleManager() {
        return impl.lifecycleManager;
    }

    /**
     * @return The DestructibleManager that controls GC cleanup for Destructibles with this framework
     */
    public DestructibleManager getDestructibleManager() {
        return impl.destructibleManager;
    }

    /*
     * Internal Callable that runs a single Task with a new
     * HardwareAccessLayerImpl instance.
     */
    private class TaskCallable<T> implements Callable<T> {
        private final Task<T> task;

        public TaskCallable(Task<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            return task.run(new HardwareAccessLayerImpl(FrameworkImpl.this));
        }
    }

    private class CreateOnscreenSurface implements Callable<OnscreenSurface> {
        private final OnscreenSurfaceOptions options;

        public CreateOnscreenSurface(OnscreenSurfaceOptions options) {
            if (options == null) {
                throw new NullPointerException("Options cannot be null");
            }
            this.options = options;
        }

        @Override
        public OnscreenSurface call() throws Exception {
            // fullscreenSurface is only ever written by the GL thread, so this is safe
            if (options.getFullscreenMode() != null && fullscreenSurface != null &&
                fullscreenSurface.get() != null) {
                throw new SurfaceCreationException(
                        "Cannot create fullscreen surface when an existing surface is fullscreen");
            }
            // minor cleanup
            if (fullscreenSurface != null && fullscreenSurface.get() == null) {
                fullscreenSurface = null;
            }

            AbstractOnscreenSurface created = impl.surfaceFactory
                                                  .createOnscreenSurface(FrameworkImpl.this, options,
                                                                         impl.contextManager
                                                                             .getSharedContext());
            impl.destructibleManager.manage(created, created.getSurfaceDestructible());
            impl.contextManager.setActiveSurface(created);

            if (created.isFullscreen()) {
                fullscreenSurface = new WeakReference<OnscreenSurface>(created);
            }
            return created;
        }
    }

    private class CreateTextureSurface implements Callable<TextureSurface> {
        private final TextureSurfaceOptions options;

        public CreateTextureSurface(TextureSurfaceOptions options) {
            if (options == null) {
                throw new NullPointerException("Options cannot be null");
            }
            this.options = options;
        }

        @Override
        public TextureSurface call() throws Exception {
            AbstractTextureSurface created = impl.surfaceFactory
                                                 .createTextureSurface(FrameworkImpl.this, options,
                                                                       impl.contextManager
                                                                           .getSharedContext());
            impl.destructibleManager.manage(created, created.getSurfaceDestructible());
            impl.contextManager.setActiveSurface(created);
            return created;
        }
    }

    private static class ManagedFramework implements DestructibleManager.ManagedDestructible {
        private final SurfaceFactory surfaceFactory;

        private final LifeCycleManager lifecycleManager;
        private final DestructibleManager destructibleManager;
        private final ResourceFactory resourceFactory;
        private final ContextManager contextManager;

        public ManagedFramework(SurfaceFactory surfaceFactory, LifeCycleManager lifecycleManager,
                                DestructibleManager destructibleManager, ResourceFactory resourceFactory,
                                ContextManager contextManager) {
            this.surfaceFactory = surfaceFactory;
            this.lifecycleManager = lifecycleManager;
            this.destructibleManager = destructibleManager;
            this.resourceFactory = resourceFactory;
            this.contextManager = contextManager;
        }

        @Override
        public Future<Void> destroy() {
            return lifecycleManager.stop(new Runnable() {
                @Override
                public void run() {
                    // Don't destroy native surface resources until the very end
                    surfaceFactory.destroy();
                }
            });
        }

        @Override
        public boolean isDestroyed() {
            return lifecycleManager.isStopped();
        }
    }
}

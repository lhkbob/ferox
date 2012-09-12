package com.ferox.renderer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.Framework;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Surface;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.Task;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * AbstractFramework is an implementation of Framework that delegates all OpenGL
 * specific operations to a {@link ResourceManager}, {@link ContextManager} and
 * {@link SurfaceFactory}. Concrete implementations must provide
 * {@link ResourceDriver ResourceDrivers} for a ResourceManager and an
 * implementation of SurfaceFactory. It is recommended that they also provide a
 * static method to create fully functioning framework that calls
 * {@link #initialize()} before passing the framework to application code.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractFramework implements Framework {
    /**
     * The name of the task group used for the convenience functions update()
     * and dispose() and other internal resource related tasks.
     */
    public static final String DEFAULT_RESOURCE_TASK_GROUP = "resource";
    
    private final CopyOnWriteArraySet<AbstractSurface> surfaces;
    
    private final SurfaceFactory surfaceFactory;
    
    private RenderCapabilities renderCaps; // final after initialize() has been called.
    
    private final LifeCycleManager lifecycleManager;
    private final ResourceManager resourceManager;
    private final ContextManager contextManager;
    
    // fullscreen support
    private final Object fullscreenLock;
    private OnscreenSurface fullscreenSurface;

    /**
     * <p>
     * Create a new AbstractFramework. The parameter, <tt>numThreads</tt>
     * defines the number of threads that the framework's ContextManager will
     * use. This constructor will also create an offscreen context that will
     * later be shared with all created surfaces for this framework. The given
     * SurfaceFactory is used to create surfaces for the Framework. The list of
     * drivers are given directly to a {@link ResourceManager}.
     * </p>
     * <p>
     * After constructing an AbstractFramework, {@link #initialize()} must be
     * invoked before it can be used.
     * </p>
     * 
     * @param surfaceFactory The SurfaceFactory used to create surfaces
     * @param numThreads The number of internal threads this framework's
     *            ContextManager will use
     * @param drivers A varargs array of ResourceDrivers that handle the
     *            low-level graphics work for resources
     * @throws NullPointerException if surfaceFactory or any driver is null
     */
    public AbstractFramework(SurfaceFactory surfaceFactory, ResourceDriver... drivers) {
        if (surfaceFactory == null)
            throw new NullPointerException("SurfaceFactory cannot be null");
        
        this.surfaceFactory = surfaceFactory;

        contextManager = new ContextManager();
        resourceManager = new ResourceManager(contextManager, drivers);
        
        surfaces = new CopyOnWriteArraySet<AbstractSurface>();
        lifecycleManager = new LifeCycleManager("ferox-renderer");
        
        fullscreenLock = new Object();
        fullscreenSurface = null;
    }

    /**
     * Complete the initialization of the framework so that the public interface
     * defined in {@link Framework} is usable. It is recommended that concrete
     * implementations of AbstractFramework define a static method that creates
     * the framework and invokes this method.
     */
    public void initialize() {
        lifecycleManager.start(new Runnable() {
            @Override
            public void run() {
                // Start up the context manager and resource manager
                contextManager.initialize(lifecycleManager, surfaceFactory);
                resourceManager.initialize(lifecycleManager);
                
                // Fetch the RenderCapabilities now, we do it this way to improve
                // the Framework creation time instead of forcing OpenGL wrappers to 
                // create and discard a context solely for capabilities detection.
                Future<RenderCapabilities> caps = contextManager.invokeOnContextThread(new Callable<RenderCapabilities>() {
                    @Override
                    public RenderCapabilities call() throws Exception {
                        OpenGLContext context = contextManager.ensureContext();
                        return context.getRenderCapabilities();
                    }
                }, false);
                
                renderCaps = getFuture(caps);
            }
        });
    }
    
    @Override
    public OnscreenSurface createSurface(final OnscreenSurfaceOptions options) {
        if (options == null)
            throw new NullPointerException("Options cannot be null");
        
        // This task is not accepted during shutdown
        Future<OnscreenSurface> create = contextManager.invokeOnContextThread(new Callable<OnscreenSurface>() {
            @Override
            public OnscreenSurface call() throws Exception {
                synchronized(fullscreenLock) {
                    if (options.getFullscreenMode() != null && fullscreenSurface != null)
                        throw new SurfaceCreationException("Cannot create fullscreen surface when an existing surface is fullscreen");
                    
                    AbstractOnscreenSurface created = surfaceFactory.createOnscreenSurface(AbstractFramework.this, options, 
                                                                                           contextManager.getSharedContext());
                    surfaces.add(created);
                    if (created.getOptions().getFullscreenMode() != null)
                        fullscreenSurface = created;
                    
                    contextManager.setActiveSurface(created, 0);
                    return created;
                }
            }
        }, false);
        return getFuture(create);
    }

    @Override
    public TextureSurface createSurface(final TextureSurfaceOptions options) {
        if (options == null)
            throw new NullPointerException("Options cannot be null");
        
        // This task is not accepted during shutdown
        Future<TextureSurface> create = contextManager.invokeOnContextThread(new Callable<TextureSurface>() {
            @Override
            public TextureSurface call() throws Exception {
                AbstractTextureSurface created = surfaceFactory.createTextureSurface(AbstractFramework.this, options, 
                                                                                     contextManager.getSharedContext());
                surfaces.add(created);

                // Mark all textures as non-disposable
                if (created.getDepthBuffer() != null)
                    resourceManager.setDisposable(created.getDepthBuffer(), false);
                for (int i = 0; i < created.getNumColorBuffers(); i++)
                    resourceManager.setDisposable(created.getColorBuffer(i), false);
                
                contextManager.setActiveSurface(created, 0);
                return created;
            }
        }, false);
        
        return getFuture(create);
    }

    @Override
    public void destroy() {
        final List<Exception> surfaceDestroyExceptions = new ArrayList<Exception>();
        lifecycleManager.stop(new Runnable() {
            @Override
            public void run() {
                // Destroy all remaining surfaces
                // The loop is structured this way so that we don't get an
                // iterator snapshot that's not updated if there were any
                // pending creates before we transitioned to STOPPING
                while(!surfaces.isEmpty()) {
                    AbstractSurface toDestroy = surfaces.iterator().next();
                    try {
                        toDestroy.destroy().get();
                    } catch(Exception e) {
                        // accumulate the exceptions but continue to destroy
                        // all of the surfaces
                        surfaceDestroyExceptions.add(e);
                    }
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Don't destroy native surface resources until the very end
                surfaceFactory.destroy();
            }
        });
        
        if (!surfaceDestroyExceptions.isEmpty()) {
            throw new FrameworkException(surfaceDestroyExceptions.size() + " exception(s) while destroying surface, first failure:", 
                                         surfaceDestroyExceptions.get(0));
        }
    }

    @Override
    public boolean isDestroyed() {
        return lifecycleManager.isStopped();
    }

    @Override
    public <T> Future<T> queue(Task<T> task) {
        // Specify false so that these tasks are only queued while the
        // state is ACTIVE
        return contextManager.invokeOnContextThread(new TaskCallable<T>(task), false);
    }

    @Override
    public Status update(Resource resource) {
        return getFuture(queue(new UpdateResourceTask(resource)));
    }

    @Override
    public void dispose(Resource resource) {
        getFuture(queue(new DisposeResourceTask(resource)));
    }

    @Override
    public Future<Void> flush(Surface surface) {
        return queue(new FlushSurfaceTask(surface));
    }

    @Override
    public void sync() {
        getFuture(queue(new EmptyTask()));
    }
    
    private <T> T getFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (CancellationException e) {
            // bury the cancel request so that the help methods don't 
            // throw exceptions when the framework is being destroyed
            return null;
        }
    }

    @Override
    public Status getStatus(Resource resource) {
        return resourceManager.getStatus(resource);
    }

    @Override
    public String getStatusMessage(Resource resource) {
        return resourceManager.getStatusMessage(resource);
    }

    @Override
    public RenderCapabilities getCapabilities() {
        return renderCaps;
    }
    
    @Override
    public DisplayMode[] getAvailableDisplayModes() {
        return surfaceFactory.getAvailableDisplayModes();
    }

    @Override
    public DisplayMode getDefaultDisplayMode() {
        return surfaceFactory.getDefaultDisplayMode();
    }

    @Override
    public OnscreenSurface getFullscreenSurface() {
        synchronized(fullscreenLock) {
            return fullscreenSurface;
        }
    }
    
    /**
     * @return The ResourceManager that handles the updates and disposals of all
     *         known resources
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * @return The ContextManager that handles threading for the contexts of all
     *         created surfaces
     */
    public ContextManager getContextManager() {
        return contextManager;
    }
    
    /**
     * @return The LifeCycleManager that controls the Framework's lifecycle
     *         implementation
     */
    public LifeCycleManager getLifeCycleManager() {
        return lifecycleManager;
    }

    /**
     * Remove the given surface from the framework's set of active lists.
     * 
     * @param surface The surface to remove
     * @throws NullPointerException if surface is null
     */
    void markSurfaceDestroyed(AbstractSurface surface) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        
        if (surfaces.remove(surface)) {
            if (surface instanceof TextureSurface) {
                // Mark all textures as non-disposable
                TextureSurface ts = (TextureSurface) surface;
                if (ts.getDepthBuffer() != null)
                    resourceManager.setDisposable(ts.getDepthBuffer(), false);
                for (int i = 0; i < ts.getNumColorBuffers(); i++)
                    resourceManager.setDisposable(ts.getColorBuffer(i), false);
            } else if (surface instanceof OnscreenSurface) {
                if (((OnscreenSurface) surface).getOptions().getFullscreenMode() != null) {
                    synchronized(fullscreenLock) {
                        // last double check (probably unneeded)
                        if (fullscreenSurface == surface)
                            fullscreenSurface = null;
                    }
                }
            }
        }
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
            return task.run(new HardwareAccessLayerImpl(AbstractFramework.this));
        }
    }
}

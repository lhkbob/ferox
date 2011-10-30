package com.ferox.renderer.impl;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ferox.input.EventQueue;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Surface;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.Task;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.GlslShader;
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
    private final OpenGLContext sharedContext;
    private RenderCapabilities renderCaps; // final after initialize() has been called.
    
    private final LifeCycleManager lifecycleManager;
    private final ResourceManager resourceManager;
    private final ContextManager contextManager;
    
    private final EventQueue eventQueue;
    
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
    public AbstractFramework(SurfaceFactory surfaceFactory, int numThreads, ResourceDriver<?>... drivers) {
        if (surfaceFactory == null)
            throw new NullPointerException("SurfaceFactory cannot be null");
        
        this.surfaceFactory = surfaceFactory;
        
        // Create the shared context now for three reasons:
        //  1. It's nice to let it be final
        //  2. If creation fails, construction fails. If it succeeds the hardware probably works well enough
        //     that we won't break later on
        //  3. We can construct the context manager here, too
        sharedContext = surfaceFactory.createShadowContext(null);
        contextManager = new ContextManager(surfaceFactory, sharedContext, numThreads);
        resourceManager = new ResourceManager(new GlslCompatibleLockComparator(), contextManager, drivers);
        
        surfaces = new CopyOnWriteArraySet<AbstractSurface>();
        lifecycleManager = new LifeCycleManager("Ferox Framework");
        
        eventQueue = new EventQueue();
        
        fullscreenLock = new Object();
        fullscreenSurface = null;
    }

    /**
     * Return an EventQueue that can be shared by all onscreen surfaces created
     * by the framework that need to generate input events.
     * 
     * @return The EventQueue for the framework
     */
    public EventQueue getEventQueue() {
        return eventQueue;
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
                contextManager.initialize(lifecycleManager);
                resourceManager.initialize(lifecycleManager);
                
                // Fetch the RenderCapabilities now, we do it this way to improve
                // the Framework creation time instead of forcing OpenGL wrappers to 
                // create and discard a context solely for capabilities detection.
                Future<RenderCapabilities> caps = contextManager.queue(new Callable<RenderCapabilities>() {
                    @Override
                    public RenderCapabilities call() throws Exception {
                        OpenGLContext context = contextManager.ensureContext();
                        return context.getRenderCapabilities();
                    }
                }, DEFAULT_RESOURCE_TASK_GROUP);
                
                renderCaps = getFuture(caps);
            }
        });
    }
    
    @Override
    public OnscreenSurface createSurface(OnscreenSurfaceOptions options) {
        lifecycleManager.getLock().lock();
        try {
            if (lifecycleManager.getStatus() == LifeCycleManager.Status.ACTIVE) {
                synchronized(fullscreenLock) {
                    if (options.getFullscreenMode() != null && fullscreenSurface != null)
                        throw new SurfaceCreationException("Cannot create fullscreen surface when an existing surface is fullscreen");
                    
                    AbstractOnscreenSurface created = surfaceFactory.createOnscreenSurface(this, options, sharedContext);
                    surfaces.add(created);
                    
                    if (created.getOptions().getFullscreenMode() != null)
                        fullscreenSurface = created;
                    return created;
                }
            } else
                return null;
        } finally {
            lifecycleManager.getLock().unlock();
        }
    }

    @Override
    public TextureSurface createSurface(TextureSurfaceOptions options) {
        lifecycleManager.getLock().lock();
        try {
            if (lifecycleManager.getStatus() == LifeCycleManager.Status.ACTIVE) {
                AbstractTextureSurface created = surfaceFactory.createTextureSurface(this, options, sharedContext);
                
                // Mark all textures as non-disposable
                if (created.getDepthBuffer() != null)
                    resourceManager.setDisposable(created.getDepthBuffer(), false);
                for (int i = 0; i < created.getNumColorBuffers(); i++)
                    resourceManager.setDisposable(created.getColorBuffer(i), false);
                
                surfaces.add(created);
                return created;
            } else
                return null;
        } finally {
            lifecycleManager.getLock().unlock();
        }
    }

    @Override
    public void destroy() {
        lifecycleManager.stop(new Runnable() {
            @Override
            public void run() {
                // This is only run after the manager has been stopped, and all
                // context threads are ended, so the surfaces array will not be
                // modified at this point
                for (AbstractSurface surface: surfaces) {
                    surface.destroy();
                }
                surfaces.clear();
                
                // Destroy the shared context
                sharedContext.destroy();
                
                // Shutdown the event queue
                eventQueue.shutdown();
            }
        });
    }

    @Override
    public boolean isDestroyed() {
        return lifecycleManager.isStopped();
    }

    @Override
    public <T> Future<T> queue(Task<T> task, String group) {
        return contextManager.queue(new TaskCallable<T>(task), group);
    }

    @Override
    public Status update(Resource resource) {
        return getFuture(queue(new UpdateResourceTask(resource), DEFAULT_RESOURCE_TASK_GROUP));
    }

    @Override
    public void dispose(Resource resource) {
        getFuture(queue(new DisposeResourceTask(resource), DEFAULT_RESOURCE_TASK_GROUP));
    }

    @Override
    public void flush(Surface surface, String group) {
        getFuture(queue(new FlushSurfaceTask(surface), group));
    }

    @Override
    public void sync(String group) {
        getFuture(queue(new EmptyTask(), group));
    }
    
    private <T> T getFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
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
    
    /*
     * Implement a Comparator<Resource> that pushes all GlslShaders to the end of the locking,
     * so that the AbstractGlslRenderer works correctly. This isn't a perfect solution but its
     * the only way to make the renderer safe in the event of bad resource updates.
     */
    private static class GlslCompatibleLockComparator implements Comparator<Resource> {
        @Override
        public int compare(Resource o1, Resource o2) {
            boolean glsl1 = o1 instanceof GlslShader;
            boolean glsl2 = o2 instanceof GlslShader;
            
            if ((glsl1 && glsl2) || (!glsl1 && !glsl2)) {
                // if both are GlslShaders or both aren't GlslShaders, order by resource id
                return o1.getId() - o2.getId();
            } else if (glsl1 && !glsl2) {
                // o1 is a GlslShader, so push it to the end
                return 1;
            } else {
                // o2 is a GlslShader, so push it to the end
                return -1;
            }
        }
    }
}

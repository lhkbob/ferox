package com.ferox.renderer.impl2;

import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Surface;
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
public class AbstractFramework implements Framework {
    private final CopyOnWriteArraySet<AbstractSurface> surfaces;
    
    private final SurfaceFactory surfaceFactory;
    private final OpenGLContextAdapter sharedContext;
    private RenderCapabilities renderCaps;
    
    private final LifeCycleManager lifecycleManager;
    private final ResourceManager resourceManager;

    private final ContextManager contextManager;

    /**
     * Create a new AbstractFramework that uses the given ResourceManager and
     * SurfaceFactory. The parameter, <tt>numThreads</tt> defines the number of
     * threads that the framework's ContextManager will use. This constructor
     * will also create an offscreen context that will later be shared with all
     * created surfaces for this framework.
     * 
     * @param resourceManager The ResourceManager this framework will use
     * @param surfaceFactory The SurfaceFactory used to create surfaces
     * @param numThreads The number of internal threads this framework's
     *            ContextManager will use
     * @throws NullPointerException if resourceManager or surfaceFactory are
     *             null
     */
    public AbstractFramework(ResourceManager resourceManager, SurfaceFactory surfaceFactory, int numThreads) {
        if (resourceManager == null)
            throw new NullPointerException("ResourceManager cannot be null");
        if (surfaceFactory == null)
            throw new NullPointerException("SurfaceFactory cannot be null");
        
        this.resourceManager = resourceManager;
        this.surfaceFactory = surfaceFactory;
        
        // Create the shared context now for three reasons:
        //  1. It's nice to let it be final
        //  2. If creation fails, construction fails. If it succeeds the hardware probably works well enough
        //     that we won't break later on
        //  3. We can construct the context manager here, too
        sharedContext = surfaceFactory.createShadowContext(null);
        contextManager = new ContextManager(surfaceFactory, sharedContext, numThreads);
        
        surfaces = new CopyOnWriteArraySet<AbstractSurface>();
        lifecycleManager = new LifeCycleManager("Ferox Framework");
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
                resourceManager.initialize(lifecycleManager, contextManager);
                
                // Fetch the RenderCapabilities now, we do it this way to improve
                // the Framework creation time instead of forcing OpenGL wrappers to 
                // create and discard a context solely for capabilities detection.
                Future<RenderCapabilities> caps = contextManager.queue(new Callable<RenderCapabilities>() {
                    @Override
                    public RenderCapabilities call() throws Exception {
                        OpenGLContextAdapter context = contextManager.ensureContext();
                        return context.getRenderCapabilities();
                    }
                }, "resource");
                
                renderCaps = getFuture(caps);
            }
        });
    }
    
    @Override
    public OnscreenSurface createSurface(OnscreenSurfaceOptions options) {
        lifecycleManager.getLock().lock();
        try {
            if (lifecycleManager.getStatus() == LifeCycleManager.Status.ACTIVE) {
                AbstractOnscreenSurface created = surfaceFactory.createOnscreenSurface(this, options, sharedContext);
                surfaces.add(created);
                return created;
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
                // FIXME: lock all textures owned by this surface
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
        return getFuture(queue(new UpdateResourceTask(resource), "resource"));
    }

    @Override
    public void dispose(Resource resource) {
        getFuture(queue(new DisposeResourceTask(resource), "resource"));
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
        // FIXME: if texturesurface, unlock all owned textures
        surfaces.remove(surface);
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

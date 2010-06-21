package com.ferox.renderer.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Surface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * AbstractFramework provides a shell for implementing the full functionality of
 * a Framework. Where possible, it separates key functionality into other
 * interfaces that it depends on, such as {@link RenderManager} and
 * {@link ResourceManager}.
 * </p>
 * <p>
 * All implemented methods of AbstractFramework are intended to be thread-safe,
 * but it requires the correct cooperation of implementations. There are a
 * number of protected methods exposed that must be used to complete the
 * functionality of the Framework.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractFramework implements Framework {
    private final ThreadLocal<List<Action>> queue;
    private volatile boolean destroyed;
    
    private ResourceManager resourceManager;
    private RenderManager renderManager;
    private RenderCapabilities renderCaps;
    
    private final Set<AbstractSurface> validSurfaces;
    
    private final ReentrantReadWriteLock stateLock;

    /**
     * Create a new AbstractFramework. This framework is not usable until its
     * {@link #init(ResourceManager, RenderManager, RenderCapabilities)} method
     * has been invoked. It's strongly recommended that subclasses call this
     * from within their constructor after super() has been invoked.
     */
    public AbstractFramework() {
        validSurfaces = new HashSet<AbstractSurface>();
        queue = new ThreadLocal<List<Action>>();
        destroyed = false;
        
        stateLock = new ReentrantReadWriteLock();
    }
    
    @Override
    public RenderCapabilities getCapabilities() {
        return renderCaps;
    }
    
     @Override
     public OnscreenSurface createSurface(OnscreenSurfaceOptions options) {
         stateLock.writeLock().lock();
         try {
             ensureNotDestroyed();
             
             OnscreenSurface s = createSurfaceImpl(options);
             if (!(s instanceof AbstractSurface)) // shouldn't happen
                 throw new UnsupportedOperationException("AbstractFramework implementation does not return a Surface of type AbstractSurface");
             
             AbstractSurface as = (AbstractSurface) s;
             Context c = as.getContext();
             if (resourceManager.getContext() == null && c != null)
                 resourceManager.setContext(c);
             
             validSurfaces.add(as);
             return s;
         } finally {
             stateLock.writeLock().unlock();
         }
     }

     @Override
     public TextureSurface createSurface(TextureSurfaceOptions options) {
         stateLock.writeLock().lock();
         try {
             ensureNotDestroyed();
             
             TextureSurface s = createSurfaceImpl(options);
             if (!(s instanceof AbstractTextureSurface)) // shouldn't happen
                 throw new UnsupportedOperationException("AbstractFramework implementation does not return a TextureSurface of type AbstractTextureSurface");
             
             AbstractTextureSurface as = (AbstractTextureSurface) s;
             Context c = as.getContext();
             if (resourceManager.getContext() == null && c != null)
                 resourceManager.setContext(c);
             
             validSurfaces.add(as);
             return s;
         } finally {
             stateLock.writeLock().unlock();
         }
     }
    
    @Override
    public Future<Void> dispose(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot cleanup a null Resource");
        
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();
            return resourceManager.scheduleDispose(resource);
        } finally {
            stateLock.readLock().unlock();
        }
    }
    
    @Override
    public void destroy(Surface surface) {
        if (surface == null)
            throw new NullPointerException("Cannot destroy a null Surface");

        stateLock.writeLock().lock();
        try {
            ensureNotDestroyed();
            
            if (surface.isDestroyed())
                return;
            if (!validSurfaces.contains(surface))
                throw new IllegalArgumentException("Cannot destroy a Surface created by another Framework");

            validSurfaces.remove(surface);
            Context c = ((AbstractSurface) surface).getContext();
            if (c != null && resourceManager.getContext() == c) {
                c = null;
                for (AbstractSurface s: validSurfaces) {
                    c = s.getContext();
                    if (c != null)
                        break;
                }
                resourceManager.setContext(c);
            }
            
            ((AbstractSurface) surface).destroy();
        } finally {
            stateLock.writeLock().unlock();
        }
    }
    
    @Override
    public void destroy() {
        stateLock.writeLock().lock();
        try {
            ensureNotDestroyed();
        
            renderManager.destroy();
            resourceManager.destroy();
            
            for (AbstractSurface s: validSurfaces)
                s.destroy();
            validSurfaces.clear();
            
            destroyImpl();
            destroyed = true;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public Status getStatus(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot retrieve Status for a null Resource");
        
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();
            return resourceManager.getStatus(resource);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public String getStatusMessage(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot retrieve status message for a null Resource");
        
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();
            return resourceManager.getStatusMessage(resource);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public void queue(Surface surface, RenderPass pass) {
        // special case when surface is a TextureSurface
        if (surface instanceof TextureSurface) {
            TextureSurface s = (TextureSurface) surface;
            queue(s, s.getActiveLayer(), s.getActiveDepthPlane(), pass);
        } else
            queue(surface, pass, new RenderPassAction(surface, pass));
    }
    
    @Override
    public void queue(TextureSurface surface, int layer, int atDepth, RenderPass pass) {
        if (!(surface instanceof AbstractTextureSurface))
            throw new IllegalArgumentException("Surface was not created by this Framework");
        
        TextureRenderPassAction action = new TextureRenderPassAction((AbstractTextureSurface) surface, pass, 
                                                                     layer, atDepth);
        if (layer < 0 || layer >= surface.getNumLayers())
            throw new IllegalArgumentException("Invalid layer argument: " + layer);
        if (atDepth < 0 || atDepth >= surface.getDepth())
            throw new IllegalArgumentException("Invalid depth argument: " + atDepth);
        queue(surface, pass, action);
    }
    
    private void queue(Surface surface, RenderPass pass, RenderPassAction action) {
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();
            
            if (surface.isDestroyed())
                return;
            if (!validSurfaces.contains(surface))
                throw new IllegalArgumentException("Surface was not created by this Framework");
            
            List<Action> threadQueue = queue.get();
            if (threadQueue == null) {
                threadQueue = new ArrayList<Action>();
                queue.set(threadQueue);
            }
            
            threadQueue.add(action);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public Future<FrameStatistics> render() {
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();

            List<Action> queuedActions = queue.get();
            queue.set(null);

            return renderManager.render(queuedActions);
        } finally {
            stateLock.readLock().unlock();
        }
    }
    
    @Override
    public FrameStatistics renderAndWait() {
        try {
            return render().get();
        } catch (InterruptedException e) {
            if (e.getCause() instanceof RenderInterruptedException)
                throw (RenderInterruptedException) e.getCause();
            else
                throw new RenderInterruptedException("Rendering was interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RenderException)
                throw (RenderException) e.getCause();
            else
                throw new RenderException("Exception occured while rendering", e);
        }
    }

    @Override
    public Future<Status> update(Resource resource, boolean forceFullUpdate) {
        if (resource == null)
            throw new NullPointerException("Cannot update a null Resource");
        stateLock.readLock().lock();
        try {
            ensureNotDestroyed();
            // perform descriptor clear now
            return resourceManager.scheduleUpdate(resource, forceFullUpdate);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Utility method that throws a RenderException if the Framework is
     * destroyed. This should be called when appropriate by subclasses when they
     * are responsible for ensuring that the Framework is not destroyed.
     */
    protected final void ensureNotDestroyed() {
        if (destroyed)
            throw new RenderException("Framework is destroyed");
    }

    /**
     * <p>
     * Initialize the AbstractFramework completely. The AbstractFramework relies
     * on a ResourceManager and a RenderManager to control the updates and
     * disposals of resources, and to complete the rendering of each frame.
     * </p>
     * <p>
     * This method is necessary because it may not be possible to create a
     * ResourceManager, RenderManager and RenderCapabilities until after
     * AbstractFramework's constructor has been invoked. It is strongly
     * recommended that subclasses call this method before their own constructor
     * completes. Undefined results occur if it's called more than once.
     * </p>
     * 
     * @param resourceManager The ResourceManager to use
     * @param renderManager The RenderManager to use
     * @param caps The RenderCapabilities of the system
     */
    // FIXME: we need to add a general initialize() method to Framework that will
    // invoke the initialize() on each manager -> so threads don't start in constructor,
    // we'll wait though until I have the FrameworkFactory ready to go since it can
    // automatically init for the caller
    protected final void init(ResourceManager resourceManager, RenderManager renderManager, 
                              RenderCapabilities caps) {
        if (resourceManager == null)
            throw new NullPointerException("ResourceManager cannot be null");
        if (renderManager == null)
            throw new NullPointerException("RenderManager cannot be null");
        
        stateLock.writeLock().lock();
        try {
            this.resourceManager = resourceManager;
            this.renderManager = renderManager;
            this.renderCaps = caps;

            resourceManager.initialize(stateLock);
            renderManager.initialize(stateLock);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * @return The RenderManager assigned by
     *         {@link #init(ResourceManager, RenderManager, RenderCapabilities)}
     *         . This is only provided for ease of the implementation.
     */
    public final RenderManager getRenderManager() {
        return renderManager;
    }

    /**
     * @return The ResourceManager assigned by
     *         {@link #init(ResourceManager, RenderManager, RenderCapabilities)}
     *         . This is only provided for ease of the implementation.
     */
    public final ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    /**
     * <p>
     * Although {@link #destroy()} is implemented and may, in fact, destroy all
     * that's necessary, destroyImpl() is provided to allow subclasses to
     * clean-up any other objects that aren't under the direct control of the
     * AbstractFramework.
     * </p>
     * <p>
     * It is not necessary to acquire any framework locks, destroy the remaining
     * surfaces, or destroy the ResourceManager and RenderManager because
     * {@link #destroy()} handles this already.
     * </p>
     */
    protected abstract void destroyImpl();

    /**
     * Create a new OnscreenSurface based on <tt>options</tt> as required by
     * {@link #createSurface(OnscreenSurfaceOptions)}. This method must return
     * an OnscreenSurface that extends from {@link AbstractSurface}. The
     * implementation does not need to worry about synchronization.
     * 
     * @param options
     * @return A valid OnscreenSurface that's also an AbstractSurface
     */
    protected abstract OnscreenSurface createSurfaceImpl(OnscreenSurfaceOptions options);

    /**
     * Create a new TextureSurface based on <tt>options</tt> as required by
     * {@link #createSurface(TextureSurfaceOptions)}. This method must return an
     * TextureSurface that extends from {@link AbstractSurface}. The
     * implementation does not need to worry about synchronization.
     * 
     * @param options
     * @return A valid TextureSurface that's also an AbstractSurface
     */
    protected abstract TextureSurface createSurfaceImpl(TextureSurfaceOptions options);
}

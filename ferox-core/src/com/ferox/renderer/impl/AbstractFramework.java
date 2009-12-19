package com.ferox.renderer.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.math.Color4f;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.resource.DirtyState;
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
	
	private final Set<RenderSurface> validSurfaces;
	
	private final Object surfaceLock;
	private final ReadWriteLock stateLock;

	/**
	 * Create a new AbstractFramework. This framework is not usable until its
	 * {@link #init(ResourceManager, RenderManager, RenderCapabilities)} method
	 * has been invoked. It's strongly recommended that subclasses call this
	 * from within their constructor after super() has been invoked.
	 */
	public AbstractFramework() {
		validSurfaces = Collections.synchronizedSet(new HashSet<RenderSurface>());
		queue = new ThreadLocal<List<Action>>();
		destroyed = false;
		
		stateLock = new ReentrantReadWriteLock();
		surfaceLock = new Object();
	}
	
	@Override
	public RenderCapabilities getCapabilities() {
		return renderCaps;
	}
	
	@Override
	public Future<Object> dispose(Resource resource) {
		if (resource == null)
			throw new NullPointerException("Cannot cleanup a null Resource");
		
		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();
			return resourceManager.scheduleDispose(resource);
		} finally {
			stateLock.readLock().unlock();
		}
	}
	
	@Override
	public void destroy(RenderSurface surface) {
		if (surface == null)
			throw new NullPointerException("Cannot destroy a null RenderSurface");

		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();
			
			synchronized(surfaceLock) {
				if (surface.isDestroyed())
					return;
				if (!validSurfaces.contains(surface))
					throw new IllegalArgumentException("Cannot destroy a RenderSurface created by another Framework");

				innerDestroy(surface);
			}
		} finally {
			stateLock.readLock().unlock();
		}
	}
	
	@Override
	public void destroy() {
		try {
			stateLock.writeLock().lock();
			ensureNotDestroyed();
		
			renderManager.destroy();
			resourceManager.destroy();
			
			Set<RenderSurface> surfaces = new HashSet<RenderSurface>(validSurfaces);
			for (RenderSurface s: surfaces) {
				destroy(s);
			}
			
			innerDestroy();
			destroyed = true;
		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public Status getStatus(Resource resource) {
		if (resource == null)
			throw new NullPointerException("Cannot retrieve Status for a null Resource");
		
		try {
			stateLock.readLock().lock();
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
		
		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();
			return resourceManager.getStatusMessage(resource);
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public void queue(RenderSurface surface, RenderPass pass) {
		List<Action> threadQueue = queue.get();
		boolean found = false;
		if (threadQueue != null) {
			int size = threadQueue.size();
			for (int i = 0; i < size; i++) {
				if (threadQueue.get(i).getRenderSurface() == surface) {
					found = true;
					break;
				}
			}
		}
		
		queue(surface, pass, !found, !found, !found);
	}

	@Override
	public void queue(RenderSurface surface, RenderPass pass, boolean clearColor, boolean clearDepth, boolean clearStencil) {
		if (surface != null) {
			queue(surface, pass, clearColor, clearDepth, clearStencil, 
				  surface.getClearColor(), surface.getClearDepth(), surface.getClearStencil());
		}
	}

	@Override
	public void queue(RenderSurface surface, RenderPass pass, boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil) {
		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();

			if (surface == null || pass == null)
				throw new NullPointerException("Cannot queue a null RenderSurface or RenderPass");
			if (depth < 0f || depth > 1f)
				throw new IllegalArgumentException("Invalid depth clear value: " + depth);
			
			if (color == null)
				color = new Color4f();
			
			// we use a simple heuristic for invalid surfaces, if it's not destroyed it must be in validSurfaces
			// if it was created by this Framework.
			if (surface.isDestroyed())
				return;
			if (!validSurfaces.contains(surface))
				throw new IllegalArgumentException("RenderSurface was not created by this Framework");

			List<Action> threadQueue = queue.get();
			if (threadQueue == null) {
				threadQueue = new LinkedList<Action>();
				queue.set(threadQueue);
			}

			if (clearColor || clearDepth || clearStencil) {
				ClearSurfaceAction c = new ClearSurfaceAction(surface, clearColor, clearDepth, clearStencil, 
															  color, depth, stencil);
				threadQueue.add(c);
			}

			RenderPassAction a = new RenderPassAction(surface, pass);
			threadQueue.add(a);
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public Future<FrameStatistics> render() {
		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();

			List<Action> queuedActions = queue.get();
			queue.set(null);

			return renderManager.render(queuedActions);
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public Future<Status> update(Resource resource, boolean forceFullUpdate) {
		if (resource == null)
			throw new NullPointerException("Cannot update a null Resource");
		
		try {
			stateLock.readLock().lock();
			ensureNotDestroyed();

			// perform descriptor clear now
			DirtyState<?> dirtyDescriptor = resource.getDirtyState();
			return resourceManager.scheduleUpdate(resource, (forceFullUpdate ? null : dirtyDescriptor));
		} finally {
			stateLock.readLock().unlock();
		}
	}

	/**
	 * Return the Lock that should be acquired before any Framework operation
	 * should be performed. Subclasses must use this when implementing the
	 * create RenderSurface methods. When this lock is acquired, any
	 * {@link #destroy()} call will block until this lock is released.
	 * 
	 * @return A Lock that prevents
	 */
	protected final Lock getFrameworkLock() {
		return stateLock.readLock();
	}

	/**
	 * <p>
	 * The surface lock is related to the framework lock, and must be acquired
	 * any time a RenderSurface is being created or being destroyed. Because
	 * {@link #destroy(RenderSurface)} correctly acquires this lock, only the
	 * create RenderSurface methods must do so. Synchronize on the returned
	 * instance to acquire the lock.
	 * </p>
	 * <p>
	 * The primary reason that this lock is necessary is to prevent the creation
	 * of a WindowSurface and a FullscreenSurface at the same time.
	 * </p>
	 * 
	 * @return The lock needed when creating or destroying surfaces
	 */
	protected final Object getSurfaceLock() {
		return surfaceLock;
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
	 * This must be called by subclasses when they successfully create a new
	 * RenderSurface.
	 * 
	 * @param surface The RenderSurface that was just created
	 */
	protected void addNotify(RenderSurface surface) {
		validSurfaces.add(surface);
	}

	/**
	 * This must be called by subclasses after they have been destroyed.
	 * 
	 * @param surface The RenderSurface that's no longer usable
	 */
	protected void removeNotify(RenderSurface surface) {
		validSurfaces.remove(surface);
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
	protected final void init(ResourceManager resourceManager, RenderManager renderManager, 
							  RenderCapabilities caps) {
		if (resourceManager == null)
			throw new NullPointerException("ResourceManager cannot be null");
		if (renderManager == null)
			throw new NullPointerException("RenderManager cannot be null");
		
		this.resourceManager = resourceManager;
		this.renderManager = renderManager;
		this.renderCaps = caps;
	}

	/**
	 * <p>
	 * Although {@link #destroy()} is implemented and may, in fact, destroy all
	 * that's necessary, innerDestroy() is provided to allow subclasses to
	 * clean-up any other objects that aren't under the direct control of the
	 * AbstractFramework.
	 * </p>
	 * <p>
	 * It is not necessary to acquire any framework locks, destroy the remaining
	 * surfaces, or destroy the ResourceManager and RenderManager because
	 * {@link #destroy()} handles this already.
	 * </p>
	 */
	protected abstract void innerDestroy();

	/**
	 * <p>
	 * Like with {@link #innerDestroy()}, this method is provided to complete
	 * any necessary destruction required for cleaning up a RenderSurface. This
	 * method can assume that s has not already been destroyed and that all
	 * necessary locks have been acquired.
	 * </p>
	 * <p>
	 * Except for those things, this method is responsible for handling anything
	 * else necessary for cleaning up the RenderSurface. This includes making
	 * any OnscreenSurfaces no longer visible, and calling
	 * {@link #removeNotify(RenderSurface)} when appropriate.
	 * </p>
	 * 
	 * @param s The RenderSurface to destroy
	 */
	protected abstract void innerDestroy(RenderSurface s);
}

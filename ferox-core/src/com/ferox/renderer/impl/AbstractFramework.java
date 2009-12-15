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

public abstract class AbstractFramework implements Framework {
	private final ThreadLocal<List<Action>> queue;
	private volatile boolean destroyed;
	
	private ResourceManager resourceManager;
	private RenderManager renderManager;
	private RenderCapabilities renderCaps;
	
	private final Set<RenderSurface> validSurfaces;
	
	private final Object surfaceLock;
	private final ReadWriteLock stateLock;
	
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
	
	protected final Lock getFrameworkLock() {
		return stateLock.readLock();
	}
	
	protected final Object getSurfaceLock() {
		return surfaceLock;
	}
	
	protected final void ensureNotDestroyed() {
		if (destroyed)
			throw new RenderException("Framework is destroyed");
	}
	
	protected void addNotify(RenderSurface surface) {
		validSurfaces.add(surface);
	}
	
	protected void removeNotify(RenderSurface surface) {
		validSurfaces.remove(surface);
	}
	
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
	
	protected abstract void innerDestroy();
	
	protected abstract void innerDestroy(RenderSurface s);
}

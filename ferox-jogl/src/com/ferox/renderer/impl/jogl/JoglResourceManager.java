package com.ferox.renderer.impl.jogl;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.media.opengl.GL2;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.UnsupportedResourceException;
import com.ferox.renderer.impl.FutureSync;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.Sync;
import com.ferox.renderer.impl.jogl.resource.GeometryHandle;
import com.ferox.renderer.impl.jogl.resource.JoglGeometryResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture1DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture2DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture3DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTextureCubeMapResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTextureRectangleResourceDriver;
import com.ferox.renderer.impl.jogl.resource.ResourceDriver;
import com.ferox.renderer.impl.jogl.resource.TextureHandle;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture1D;
import com.ferox.resource.Texture2D;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.Resource.DirtyState;
import com.ferox.resource.Resource.Status;

public class JoglResourceManager implements ResourceManager {
	private static final long CLEANUP_WAKEUP_INTERVAL = 500;
	
	private static int threadId = 0;
	private static final AtomicReferenceFieldUpdater<JoglResourceManager, Boolean> casDestroyed =
		AtomicReferenceFieldUpdater.newUpdater(JoglResourceManager.class, boolean.class, "destroyed");
	
	private final JoglFramework framework;
	private final Thread workerThread;
	private final Thread cleanupScheduler;
	
	// must be volatile so everything can see changes to it
	private volatile boolean destroyed;
	
	private final ResourceDriver geometryDriver;
	private final ResourceDriver t1dDriver;
	private final ResourceDriver t2dDriver;
	private final ResourceDriver t3dDriver;
	private final ResourceDriver tcmDriver;
	private final ResourceDriver trDriver;
	
	private final Set<WeakResourceReference> residentResources; // needed for auto-cleanup
	private final Set<Resource> locks;
	
	private final Object updateLock = new Object();
	private final Deque<Sync<?>> pendingTasks; // not thread-safe, must be synchronized
	
	public JoglResourceManager(JoglFramework framework, RenderCapabilities caps) {
		if (framework == null)
			throw new NullPointerException("Cannot specify a null JoglFramework");
		this.framework = framework;
		
		geometryDriver = new JoglGeometryResourceDriver(caps);
		t1dDriver = new JoglTexture1DResourceDriver(caps);
		t2dDriver = new JoglTexture2DResourceDriver(caps);
		t3dDriver = new JoglTexture3DResourceDriver(caps);
		tcmDriver = new JoglTextureCubeMapResourceDriver(caps);
		trDriver = new JoglTextureRectangleResourceDriver(caps);
		
		residentResources = new HashSet<WeakResourceReference>();
		locks = Collections.synchronizedSet(new HashSet<Resource>());
		pendingTasks = new ArrayDeque<Sync<?>>();
		
		destroyed = false;
		
		workerThread = new Thread(new ResourceWorker());
		workerThread.setName("resource-worker " + (threadId++));
		workerThread.setDaemon(true);
		
		cleanupScheduler = new Thread(new DisposalScheduler());
		cleanupScheduler.setName("disposal-scheduler " + (threadId++));
		cleanupScheduler.setDaemon(true);
		
		// start everything
		workerThread.start();
		cleanupScheduler.start();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ResourceHandle getHandle(Resource resource) {
		if (resource == null)
			throw new NullPointerException("Cannot retreive handle for null Resource");
		ResourceDriver driver = getDriver(resource);
		if (driver == null)
			throw new UnsupportedResourceException("Resource type: " + resource.getClass());
		
		ResourceData rd = (ResourceData) resource.getRenderData(framework);
		DirtyState<?> ds = resource.getDirtyState();
		
		if (rd == null || rd.queuedSync != null || ds != null) {
			// something is going on, so check everything
			synchronized(pendingTasks) {
				// re-fetch rd to get any possible changes
				rd = (ResourceData) resource.getRenderData(framework);
				if (rd == null) {
					// do a new update
					rd = new ResourceData();
					resource.setRenderData(framework, rd);
					
					UpdateTask ut = new UpdateTask(resource, null, driver);
					synchronizeUpdate(new Sync<Status>(ut));
				} else if (rd.queuedTask instanceof UpdateTask) {
					// merge the dirty states
					UpdateTask ut = (UpdateTask) rd.queuedTask;
					if (ds != null) // only merge if we have to
						ut.dirtyState = merge(ut.dirtyState, ds);
					
					pendingTasks.remove(rd.queuedSync);
					synchronizeUpdate((Sync<Status>) rd.queuedSync);
				} else if (rd.queuedTask instanceof DisposeTask) {
					// cleanup is still pending, so return null so we don't get
					// a destroyed resource bound to some other context
					return null;
				} else if (ds != null) {
					// perform an update to get the new dirty state 
					UpdateTask ut = new UpdateTask(resource, ds, driver);
					synchronizeUpdate(new Sync<Status>(ut));
				}
			}
		}
		
		// if we're lucky we didn't have to synchronize
		return (rd.handle.getStatus() == Status.READY ? rd.handle : null);
	}
	
	// assumes that pendingTasks is already locked
	private void synchronizeUpdate(Sync<Status> updateSync) {
		if (JoglContext.getCurrent() == framework.getShadowContext()) {
			updateSync.run();
		} else {
			pendingTasks.addFirst(updateSync);
			pendingTasks.notifyAll();
			try {
				synchronized(updateLock) {
					while(!updateSync.isDone())
						updateLock.wait();
				}
			} catch(InterruptedException ie) {
				throw new RenderInterruptedException("Interrupted while waiting for Resource", ie);
			}
		}
	}

	@Override
	public Status getStatus(Resource resource) {
		ResourceDriver d = getDriver(resource);
		if (d == null)
			return Status.UNSUPPORTED;
		
		ResourceData rd = (ResourceData) resource.getRenderData(framework);
		if (rd == null || rd.handle == null || rd.handle.getStatus() == Status.DISPOSED)
			return Status.DISPOSED;
		else
			return rd.handle.getStatus();
	}

	@Override
	public String getStatusMessage(Resource resource) {
		ResourceDriver d = getDriver(resource);
		if (d == null)
			return "Resource of type " + resource.getClass().getSimpleName() + " is unsupported";
		
		ResourceData rd = (ResourceData) resource.getRenderData(framework);
		if (rd == null || rd.handle == null || rd.handle.getStatus() == Status.DISPOSED)
			return "Resource is disposed of";
		else
			return rd.handle.getStatusMessage();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Future<Object> scheduleDispose(Resource resource) {
		ResourceDriver d = getDriver(resource);
		if (d == null)
			throw new UnsupportedResourceException("Resource type: " + resource.getClass());
		if (locks.contains(resource))
			throw new IllegalArgumentException("Cannot dispose of a locked Resource");
		
		synchronized(pendingTasks) {
			if (destroyed)
				throw new RenderException("Cannot retrieve ResourceHandle from a destroyed JoglResourceManager");

			ResourceData rd = (ResourceData) resource.getRenderData(framework);
			if (rd == null || rd.handle == null || rd.handle.getStatus() == Status.DISPOSED)
				return new FutureSync<Object>(null);

			Callable<?> task = rd.queuedTask;
			if (task instanceof DisposeTask) {
				// re-use the existing sync
				return new FutureSync<Object>((Sync<Object>) rd.queuedSync);
			} else if (task instanceof UpdateTask) {
				// must cancel the update
				rd.queuedSync.cancel(false);
				pendingTasks.remove(rd.queuedSync);
			}

			task = new DisposeTask(resource, d);
			rd.queuedTask = task;
			rd.queuedSync = new Sync<Object>((Callable<Object>) task);
			pendingTasks.add(rd.queuedSync);
			
			pendingTasks.notifyAll();
			return new FutureSync<Object>((Sync<Object>) rd.queuedSync);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Future<Status> scheduleUpdate(Resource resource, DirtyState<?> dirtyDescriptor) {
		ResourceDriver d = getDriver(resource);
		if (d == null)
			throw new UnsupportedResourceException("Resoure type: " + resource.getClass());
		
		ResourceData rd;
		synchronized(pendingTasks) {
			if (destroyed)
				throw new RenderException("Cannot retrieve ResourceHandle from a destroyed JoglResourceManager");

			rd = (ResourceData) resource.getRenderData(framework);
			if (rd == null) {
				rd = new ResourceData();
				resource.setRenderData(framework, null);
			}
			
			if (rd != null && rd.queuedTask instanceof UpdateTask) {
				// merge dirty states
				UpdateTask ut = (UpdateTask) rd.queuedTask;
				ut.dirtyState = merge(ut.dirtyState, dirtyDescriptor);

				return new FutureSync<Status>((Sync<Status>) rd.queuedSync);
			} else if (rd != null && rd.queuedTask instanceof DisposeTask) {
				// cancel it
				rd.queuedSync.cancel(false);
				pendingTasks.remove(rd.queuedSync);
			}
			
			UpdateTask task = new UpdateTask(resource, dirtyDescriptor, d);
			Sync<Status> sync = new Sync<Status>(task);
			
			rd.queuedSync = sync;
			rd.queuedTask = task;
			pendingTasks.add(sync);
			
			pendingTasks.notifyAll();
			return new FutureSync<Status>(sync);
		}
	}

	@SuppressWarnings("unchecked")
	private DirtyState<?> merge(DirtyState<?> d1, DirtyState<?> d2) {
		if (d1 != null && d2 != null)
			return ((DirtyState) d1).merge((DirtyState) d2); // lame
		else
			return null;
	}
	
	@Override
	public void destroy() {
		if (!casDestroyed.compareAndSet(this, false, true))
			return;
		destroyed = true;
		
		try {
			workerThread.interrupt();
			cleanupScheduler.interrupt();
			
			if (Thread.currentThread() != workerThread)
				workerThread.join();
			if (Thread.currentThread() != cleanupScheduler)
				cleanupScheduler.join();
		} catch(InterruptedException ie) {
			// do nothing
		}
		
		synchronized(pendingTasks) {
			for (Sync<?> s: pendingTasks)
				s.cancel(false);
			pendingTasks.clear();
		}
		
		synchronized(updateLock) {
			// just in case
			updateLock.notifyAll();
		}
	}
	
	public void lock(Resource resource) {
		if (resource == null)
			throw new NullPointerException("Cannot lock a null Resource");
		locks.add(resource);
	}
	
	public void unlock(Resource resource) {
		if (resource == null)
			throw new NullPointerException("Cannot unlock a null Resource");
		locks.remove(resource);
	}
	
	private ResourceDriver getDriver(Resource r) {
		if (r instanceof Geometry)
			return geometryDriver;
		else if (r instanceof Texture1D)
			return t1dDriver;
		else if (r instanceof Texture2D)
			return t2dDriver;
		else if (r instanceof Texture3D)
			return t3dDriver;
		else if (r instanceof TextureRectangle)
			return trDriver;
		else if (r instanceof TextureCubeMap)
			return tcmDriver;
		else
			return null;
	}
	
	private ResourceDriver getDriver(ResourceHandle handle) {
		if (handle instanceof GeometryHandle) {
			return geometryDriver;
		} else if (handle instanceof TextureHandle) {
			switch(((TextureHandle) handle).glTarget) {
			case GL2.GL_TEXTURE_1D: return t1dDriver;
			case GL2.GL_TEXTURE_2D: return t2dDriver;
			case GL2.GL_TEXTURE_3D: return t3dDriver;
			case GL2.GL_TEXTURE_CUBE_MAP: return tcmDriver;
			case GL2.GL_TEXTURE_RECTANGLE_ARB: return trDriver;
			}
		}
		
		return null;
	}
	
	private class ResourceWorker implements Runnable {
		@Override
		public void run() {
			while(!destroyed) {
				try {
					synchronized(pendingTasks) {
						while(pendingTasks.isEmpty())
							pendingTasks.wait();
						
						Sync<?> sync = pendingTasks.removeFirst();
						sync.run();
					}
				} catch(InterruptedException ie) {
					// do nothing
				}
			}
		}
	}
	
	private class DisposalScheduler implements Runnable {
		@Override
		public void run() {
			while(!destroyed) {
				try {
					synchronized(residentResources) {
						Iterator<WeakResourceReference> it = residentResources.iterator();
						while(it.hasNext()) {
							WeakResourceReference next = it.next();
							if (next.get() == null) {
								// schedule cleanup
								ResourceHandle handle = next.handle;
								if (handle.getStatus() != Status.DISPOSED) {
									synchronized(pendingTasks) {
										pendingTasks.add(new Sync<Object>(new HandleDisposeTask(handle, getDriver(handle))));
										pendingTasks.notifyAll();
									}
								}
								it.remove();
							} else if (next.handle.getStatus() == Status.DISPOSED)
								it.remove(); // clean up explicit dispose
						}
					}
					
					// sleep and check periodically
					Thread.sleep(CLEANUP_WAKEUP_INTERVAL);
				} catch(InterruptedException ie) {
					// do nothing
				}
			}
		}
	}
	
	private class UpdateTask implements Callable<Status> {
		private final Resource toUpdate;
		private final ResourceDriver driver;
		
		private DirtyState<?> dirtyState;
		
		public UpdateTask(Resource toUpdate, DirtyState<?> dirtyState, ResourceDriver driver) {
			this.toUpdate = toUpdate;
			this.dirtyState = dirtyState;
			this.driver = driver;
		}
		
		@Override
		public Status call() throws Exception {
			ResourceData rd = (ResourceData) toUpdate.getRenderData(framework);
			ResourceHandle handle = rd.handle;

			if (handle == null || handle.getStatus() == Status.DISPOSED) {
				// init resource
				handle = driver.init(toUpdate);
				rd.handle = handle;
				synchronized(residentResources) {
					residentResources.add(new WeakResourceReference(toUpdate, handle));
				}
			} else {
				// perform an update
				try {
					driver.update(toUpdate, handle, dirtyState);
				} catch(RuntimeException re) {
					handle.setStatus(Status.ERROR);
					handle.setStatusMessage("Exception thrown during update");
					throw re;
				}
			}

			if (rd.queuedTask == this) {
				// reset data since it's this task
				rd.queuedSync = null;
				rd.queuedTask = null;
			}

			updateLock.notifyAll(); // wake-up any thread blocking on getHandle()
			return handle.getStatus();
		}
	}
	
	private class DisposeTask implements Callable<Object> {
		private final Resource toDispose;
		private final ResourceDriver driver;
		
		public DisposeTask(Resource toDispose, ResourceDriver driver) {
			this.toDispose = toDispose;
			this.driver = driver;
		}
		
		@Override
		public Object call() throws Exception {
			ResourceData rd = (ResourceData) toDispose.getRenderData(framework);
			toDispose.setRenderData(framework, null);

			if (rd != null && rd.handle != null && rd.handle.getStatus() != Status.DISPOSED) {
				// dispose resource
				driver.dispose(rd.handle);
				rd.handle.setStatus(Status.DISPOSED);
				// disposalScheduler will automatically remove handle from residentResources
			}

			if (rd.queuedTask == this) {
				// reset data since it's this task
				rd.queuedSync = null;
				rd.queuedTask = null;
			}
			// dummy return value
			return null;
		}
	}
	
	private class HandleDisposeTask implements Callable<Object> {
		private final ResourceHandle handle;
		private final ResourceDriver driver;
		
		public HandleDisposeTask(ResourceHandle handle, ResourceDriver driver) {
			this.handle = handle;
			this.driver = driver;
		}
		
		@Override
		public Object call() throws Exception {
			if (handle.getStatus() != Status.DISPOSED) {
				driver.dispose(handle);
				handle.setStatus(Status.DISPOSED);
			}
			return null;
		}
	}
	
	private class ResourceData {
		ResourceHandle handle;
		
		volatile Callable<?> queuedTask; // Callable passed to queuedSync
		volatile Sync<?> queuedSync;
	}
	
	private class WeakResourceReference extends WeakReference<Resource> {
		final ResourceHandle handle;
		
		public WeakResourceReference(Resource resource, ResourceHandle handle) {
			super(resource);
			this.handle = handle;
		}
	}
}

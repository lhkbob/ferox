package com.ferox.renderer.impl.jogl;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.UnsupportedResourceException;
import com.ferox.renderer.impl.FutureSync;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.Sync;
import com.ferox.renderer.impl.jogl.resource.JoglGeometryResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture1DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture2DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTexture3DResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTextureCubeMapResourceDriver;
import com.ferox.renderer.impl.jogl.resource.JoglTextureRectangleResourceDriver;
import com.ferox.renderer.impl.jogl.resource.ResourceDriver;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture1D;
import com.ferox.resource.Texture2D;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.Resource.Status;

public class JoglResourceManager implements ResourceManager {
	private static final Logger log = Logger.getLogger(JoglFramework.class.getPackage().getName());
	private static final long CLEANUP_WAKEUP_INTERVAL = 500;
	
	private static int threadId = 0;
	private static final AtomicReferenceFieldUpdater<JoglResourceManager, Boolean> casDestroyed =
		AtomicReferenceFieldUpdater.newUpdater(JoglResourceManager.class, Boolean.class, "destroyed");
	
	private final JoglFramework framework;
	private final Thread workerThread;
	private final Thread cleanupScheduler;
	
	// must be volatile so everything can see changes to it
	private volatile Boolean destroyed;
	
	// resource data per resource, accessed by id
	private ResourceData[] resourceData;
	private final Map<Class<? extends Resource>, ResourceDriver> drivers;
	
	private final Set<WeakResourceReference> residentResources; // needed for auto-cleanup
	private final Set<Resource> locks;
	
	private final Object updateLock = new Object();
	private final Deque<Sync<?>> pendingTasks; // not thread-safe, must be synchronized
	
	public JoglResourceManager(JoglFramework framework, RenderCapabilities caps) {
		if (framework == null)
			throw new NullPointerException("Cannot specify a null JoglFramework");
		if (caps == null)
			throw new NullPointerException("Cannot specify a null RenderCapabilities");
		this.framework = framework;
		
		drivers = new HashMap<Class<? extends Resource>, ResourceDriver>();
		drivers.put(Geometry.class, new JoglGeometryResourceDriver(caps));
		drivers.put(Texture1D.class, new JoglTexture1DResourceDriver(caps));
		drivers.put(Texture2D.class, new JoglTexture2DResourceDriver(caps));
		drivers.put(Texture3D.class, new JoglTexture3DResourceDriver(caps));
		drivers.put(TextureCubeMap.class, new JoglTextureCubeMapResourceDriver(caps));
		drivers.put(TextureRectangle.class, new JoglTextureRectangleResourceDriver(caps));
		resourceData = new ResourceData[0];
		
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
		
		ResourceData rd = getResourceData(resource);
		DirtyState<?> ds = resource.getDirtyState();
		
		if (rd == null || rd.queuedSync != null || ds != null) {
			ResourceDriver driver = getDriver(resource);
			if (driver == null)
				throw new UnsupportedResourceException("Resource type: " + resource.getClass());
			
			log.log(Level.FINE, "getHandle() request must block for potential update", resource);
			// something is going on, so check everything
			synchronized(pendingTasks) {
				// re-fetch rd to get any possible changes
				rd = getResourceData(resource);
				if (rd == null) {
					// do a new update
					rd = new ResourceData(resource.getId(), driver);
					setResourceData(resource, rd);
					
					UpdateTask ut = new UpdateTask(resource, null);
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
					UpdateTask ut = new UpdateTask(resource, ds);
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
				log.log(Level.SEVERE, "Interrupted while waiting for ResourceHandle", ie);
				throw new RenderInterruptedException("Interrupted while waiting for Resource", ie);
			}
		}
	}

	@Override
	public Status getStatus(Resource resource) {
		ResourceDriver d = getDriver(resource);
		if (d == null)
			return Status.UNSUPPORTED;
		
		ResourceData rd = getResourceData(resource);
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
		
		ResourceData rd = getResourceData(resource);
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

			ResourceData rd = getResourceData(resource);
			if (rd == null || rd.handle == null || rd.handle.getStatus() == Status.DISPOSED)
				return new FutureSync<Object>(null); // already cleaned up

			Callable<?> task = rd.queuedTask;
			if (task instanceof DisposeTask) {
				// re-use the existing sync
				return new FutureSync<Object>((Sync<Object>) rd.queuedSync);
			} else if (task instanceof UpdateTask) {
				// must cancel the update
				rd.queuedSync.cancel(false);
				pendingTasks.remove(rd.queuedSync);
			}

			task = new DisposeTask(resource);
			rd.queuedTask = task;
			rd.queuedSync = new Sync<Object>((Callable<Object>) task);
			pendingTasks.add(rd.queuedSync);
			
			try {
				log.log(Level.INFO, "Scheduling disposal of resource " + resource.getClass().getSimpleName() + ":" + resource.getId());
				return new FutureSync<Object>((Sync<Object>) rd.queuedSync);
			} finally {
				pendingTasks.notifyAll();
			}
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

			rd = getResourceData(resource);
			if (rd == null) {
				rd = new ResourceData(resource.getId(), d);
				setResourceData(resource, rd);
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
			
			UpdateTask task = new UpdateTask(resource, dirtyDescriptor);
			Sync<Status> sync = new Sync<Status>(task);
			
			rd.queuedSync = sync;
			rd.queuedTask = task;
			pendingTasks.add(sync);
			
			try {
				log.log(Level.INFO, "Scheduling update for resource " + resource.getClass().getSimpleName() + ":" + resource.getId());
				return new FutureSync<Status>(sync);
			} finally {
				pendingTasks.notifyAll();
			}
		}
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
			log.log(Level.WARNING, "Interrupted while waiting for work threads to complete");
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
	
	/* Private helper methods */
	
	private ResourceDriver getDriver(Resource r) {
		Class<?> clazz = r.getClass();
		while(clazz != null && Resource.class.isAssignableFrom(clazz)) {
			ResourceDriver d = drivers.get(clazz);
			if (d != null)
				return d;
			clazz = clazz.getSuperclass();
		}
		return null;
	}
	
	private ResourceData getResourceData(Resource r) {
		int id = r.getId();
		return (id < resourceData.length ? resourceData[id] : null);
	}
	
	private void setResourceData(Resource r, ResourceData rd) {
		int id = r.getId();
		if (id >= resourceData.length && rd != null)
			resourceData = Arrays.copyOf(resourceData, Math.max(resourceData.length * 2, id + 1));
		if (id < resourceData.length)
			resourceData[id] = rd;
	}
	
	@SuppressWarnings("unchecked")
	private DirtyState<?> merge(DirtyState<?> d1, DirtyState<?> d2) {
		if (d1 != null && d2 != null)
			return ((DirtyState) d1).merge((DirtyState) d2); // lame
		else
			return null;
	}
	
	/* Inner classes that provide much grunt work */
	
	private class ResourceWorker implements Runnable {
		@Override
		public void run() {
			while(!destroyed) {
				try {
					synchronized(pendingTasks) {
						while(pendingTasks.isEmpty())
							pendingTasks.wait();
						
						Sync<?> sync = pendingTasks.removeFirst();
						framework.getShadowContext().runSync(sync);
					}
				} catch(InterruptedException ie) {
					// do nothing
					log.log(Level.WARNING, "ResourceManager interrupted");
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
							ResourceData rd = next.data;

							if (next.get() == null) {
								// schedule cleanup
								if (rd.handle.getStatus() != Status.DISPOSED) {
									synchronized(pendingTasks) {
										log.log(Level.INFO, "Scheduling disposal for garbage collected resource with id=" + rd.id);
										pendingTasks.add(new Sync<Object>(new HandleDisposeTask(rd)));
										pendingTasks.notifyAll();
									}
								}
								it.remove();
							} else if (rd.handle.getStatus() == Status.DISPOSED)
								it.remove(); // clean up explicit dispose
						}
					}
					
					// sleep and check periodically
					Thread.sleep(CLEANUP_WAKEUP_INTERVAL);
				} catch(InterruptedException ie) {
					// do nothing
					log.log(Level.WARNING, "DisposalScheduler interrupted");
				}
			}
		}
	}
	
	private class UpdateTask implements Callable<Status> {
		private final Resource toUpdate;
		
		private DirtyState<?> dirtyState;
		
		public UpdateTask(Resource toUpdate, DirtyState<?> dirtyState) {
			this.toUpdate = toUpdate;
			this.dirtyState = dirtyState;
		}
		
		@Override
		public Status call() throws Exception {
			long now = System.currentTimeMillis();
			ResourceData rd = getResourceData(toUpdate);
			ResourceHandle handle = rd.handle;

			if (handle == null || handle.getStatus() == Status.DISPOSED) {
				// init resource
				handle = rd.driver.init(toUpdate);
				rd.handle = handle;
				synchronized(residentResources) {
					residentResources.add(new WeakResourceReference(toUpdate, rd));
				}
			} else {
				// perform an update
				try {
					rd.driver.update(toUpdate, handle, dirtyState);
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

			Status status = handle.getStatus();
			synchronized(updateLock) {
				updateLock.notifyAll(); // wake-up any thread blocking on getHandle()
			}
			log.log(Level.INFO, "Update complete for " + toUpdate.getClass().getSimpleName() + ":" + toUpdate.getId() + " in " + (System.currentTimeMillis() - now) + " ms");
			return status;
		}
	}
	
	private class DisposeTask implements Callable<Object> {
		private final Resource toDispose;
		
		public DisposeTask(Resource toDispose) {
			this.toDispose = toDispose;
		}
		
		@Override
		public Object call() throws Exception {
			long now = System.currentTimeMillis();
			ResourceData rd = getResourceData(toDispose);
			setResourceData(toDispose, null);

			if (rd != null && rd.handle != null && rd.handle.getStatus() != Status.DISPOSED) {
				// dispose resource
				rd.driver.dispose(rd.handle);
				rd.handle.setStatus(Status.DISPOSED);
				// disposalScheduler will automatically remove handle from residentResources
			}

			if (rd.queuedTask == this) {
				// reset data since it's this task
				rd.queuedSync = null;
				rd.queuedTask = null;
			}
			// dummy return value
			log.log(Level.INFO, "Disposal complete for " + toDispose.getClass().getSimpleName() + ":" + toDispose.getId() + " in " + (System.currentTimeMillis() - now) + " ms");
			return null;
		}
	}
	
	private class HandleDisposeTask implements Callable<Object> {
		private final ResourceData data;
		
		public HandleDisposeTask(ResourceData rd) {
			data = rd;
		}
		
		@Override
		public Object call() throws Exception {
			ResourceHandle handle = data.handle;
			
			if (handle.getStatus() != Status.DISPOSED) {
				data.driver.dispose(handle);
				handle.setStatus(Status.DISPOSED);
			}
			
			// can't use setResourceData since we don't have a Resource
			if (data.id < resourceData.length)
				resourceData[data.id] = null;
			return null;
		}
	}
	
	/* These classes can be static */
	
	private static class ResourceData {
		final ResourceDriver driver;
		final int id;
		
		ResourceHandle handle;
		
		volatile Callable<?> queuedTask; // Callable passed to queuedSync
		volatile Sync<?> queuedSync;
		
		public ResourceData(int id, ResourceDriver driver) {
			this.id = id;
			this.driver = driver;
		}
	}
	
	private static class WeakResourceReference extends WeakReference<Resource> {
		final ResourceData data;
		
		public WeakResourceReference(Resource resource, ResourceData rd) {
			super(resource);
			data = rd;
		}
	}
}

package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.FutureSync;
import com.ferox.renderer.impl.RenderManager;
import com.ferox.renderer.impl.Sync;

/**
 * JoglRenderManager is a complete implementation of RenderManager. It has two
 * possible modes of operation:
 * <ol>
 * <li>It can serialize all render requests onto a single internal thread.</li>
 * <li>It can execute each render request on the thread that called it.</li>
 * </ol>
 * 
 * @author Michael Ludwig
 */
public class JoglRenderManager implements RenderManager {
	private static final Logger log = Logger.getLogger(JoglFramework.class.getPackage().getName());
	
	private static int threadId = 0;
	private static final AtomicReferenceFieldUpdater<JoglRenderManager, Boolean> casDestroyed =
		AtomicReferenceFieldUpdater.newUpdater(JoglRenderManager.class, Boolean.class, "destroyed");
	
	private final JoglFramework framework;
	private final Thread workerThread;
	
	// must be volatile so everything can see changes to it
	private volatile Boolean destroyed;
	private volatile BlockingQueue<Sync<FrameStatistics>> renderQueue;

	/**
	 * Create a new JoglRenderManager that will be used by the given
	 * JoglFramework. If serialize is true, all renders occur on a single inner
	 * thread. If it is false, then all renders occur on the thread that invoked
	 * {@link #render(List)}.
	 * 
	 * @param framework The JoglFramework to use
	 * @param serialize Render serialization policy
	 * @throws NullPointerException if framework is null
	 */
	public JoglRenderManager(JoglFramework framework, boolean serialize) {
		if (framework == null)
			throw new NullPointerException("Cannot specify a null Framework");
		this.framework = framework;
		renderQueue = new LinkedBlockingQueue<Sync<FrameStatistics>>();
		destroyed = false;
		
		if (serialize) {
			workerThread = new Thread(new RenderWorker());
			workerThread.setName("render-worker " + (threadId++));
			workerThread.setDaemon(true);

			workerThread.start();
		} else
			workerThread = null;
	}
	
	@Override
	public Future<FrameStatistics> render(List<Action> actions) {
		RenderCallable render = new RenderCallable(actions);
		Sync<FrameStatistics> sync = new Sync<FrameStatistics>(render);
		
		// we use the null status of renderQueue as a proxy for being destroyed
		BlockingQueue<Sync<FrameStatistics>> queue = renderQueue;
		if (queue != null)
			queue.add(sync);
		else
			sync.cancel(false);
		
		// FIXME: if we're running on current thread, don't bother queuing
		// it up (see above)
		if (workerThread == null) {
			// not serialized, so run it on the current thread
			sync.run();
			queue.remove(sync);
		}
		
		return new FutureSync<FrameStatistics>(sync);
	}
	
	@Override
	public void destroy() {
		// guarantees only one thread gets to destroy the render manager
		if (!casDestroyed.compareAndSet(this, false, true))
			return;
		
		if (workerThread != null) {
			try {
				// interrupt it in case it's rendering or sleeping
				workerThread.interrupt();
				if (Thread.currentThread() != workerThread)
					workerThread.join();
			} catch (InterruptedException e) {
				// continue
				log.log(Level.WARNING, "Exception while waiting for worker thread to terminate", e);
			}
		}
		
		// cancel all tasks
		BlockingQueue<Sync<FrameStatistics>> queue = renderQueue;
		renderQueue = null;
		
		for(Sync<FrameStatistics> s: queue) {
			s.cancel(true);
		}
	}
	
	private class RenderWorker implements Runnable {
		@Override
		public void run() {
			while(!destroyed) {
				try {
					Sync<FrameStatistics> render = renderQueue.take();
					if (render != null)
						render.run(); // does nothing if it's cancelled
				} catch(InterruptedException ie) {
					// continue
					log.log(Level.WARNING, "Render interrupted");
				}
			}
		}
	}
	
	private class RenderCallable implements Callable<FrameStatistics> {
		final List<Action> actions;

		public RenderCallable(List<Action> actions) {
			this.actions = actions;
		}
		
		@Override
		public FrameStatistics call() throws Exception {
			FrameStatistics stats = new FrameStatistics();
			long now = System.nanoTime();
			
			if (actions != null) {
				Iterator<Action> it = actions.iterator();
				while(it.hasNext()) {
					Action a = it.next();
					if (!a.prepare())
						it.remove();
				}
			}
			
			if (actions != null && !actions.isEmpty()) {
				JoglContext batchOwner = null;
				JoglRenderSurface lastSurface = null; // may not be batchOwner
				List<Action> batch = new ArrayList<Action>();
				
				JoglRenderSurface s;
				for (Action a: actions) {
					s = (JoglRenderSurface) a.getRenderSurface();
					if (s == null || s == lastSurface) {
						// just add it
						batch.add(a);
					} else {
						// add post action for last surface
						if (lastSurface != null)
							batch.add(lastSurface.getPostRenderAction());
						
						if (s.getContext() != null && batchOwner != s.getContext()) {
							// run the current batch
							if (batch.size() > 0 && batchOwner != null) {
								renderBatch(batchOwner, batch, stats);
								batch = new ArrayList<Action>();
							}
							
							batchOwner = s.getContext();
						}
						
						// start a new surface
						batch.add(s.getPreRenderAction());
						batch.add(a);
						
						lastSurface = s;
					}
				}
				
				if (batch.size() > 0) {
					if (lastSurface != null)
						batch.add(lastSurface.getPostRenderAction());
					renderBatch(batchOwner, batch, stats);
				}
			}
			
			stats.setRenderTime(System.nanoTime() - now);
			log.log(Level.FINE, "Render completed in " + (stats.getRenderTime() / 1000000) + " ms");
			return stats;
		}
		
		private void renderBatch(JoglContext batchOwner, List<Action> actions, FrameStatistics stats) throws InterruptedException {
			if (batchOwner == null)
				batchOwner = framework.getShadowContext();
			
			if (Thread.interrupted())
				throw new InterruptedException();
			batchOwner.render(actions, stats);
		}
	}
}

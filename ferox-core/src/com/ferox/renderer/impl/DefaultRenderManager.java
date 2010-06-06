package com.ferox.renderer.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.FrameStatistics;

/**
 * DefaultRenderManager is a complete implementation of RenderManager that
 * should be suitable for most purposes of Framework implementations. It was
 * designed to work hand-in-hand with the DefaultResourceManager, although it
 * should be possible to use a different ResourceManager with this render
 * manager.
 * 
 * @author Michael Ludwig
 */
public class DefaultRenderManager implements RenderManager {
    private static int threadId = 0;
    
    private volatile ReentrantReadWriteLock frameworkLock;
    
    private final AtomicBoolean destroyed;
    private final BlockingQueue<Sync<FrameStatistics>> queue;
    
    private final Thread renderThread;
    private final ResourceManager resourceManager;

    /**
     * Create a new DefaultRenderManager that is tied to the given
     * DefaultResourceManager. This manager must be the resource manager that is
     * used by the AbstractFramework that will also be using this created
     * RenderManager.
     * 
     * @param resourceManager The resource manager to be paired with
     * @throws NullPointerException if resourceManager is null
     */
    public DefaultRenderManager(ResourceManager resourceManager) {
        if (resourceManager == null)
            throw new NullPointerException("ResourceManager must be non-null");
        this.resourceManager = resourceManager;
        
        destroyed = new AtomicBoolean(false);
        queue = new ArrayBlockingQueue<Sync<FrameStatistics>>(4);
        
        renderThread = new Thread(new RenderWorker());
        renderThread.setName("render-worker " + (++threadId));
        renderThread.setDaemon(true);
    }
    
    @Override
    public void initialize(ReentrantReadWriteLock lock) {
        if (lock == null)
            throw new NullPointerException("Lock cannot be null");
        if (destroyed.get() || frameworkLock != null)
            throw new IllegalStateException("Cannot re-initialize the RenderManager");
        
        frameworkLock = lock;
        renderThread.start();
    }
    
    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true))
            return; // already destroyed
        
        try {
            renderThread.interrupt();
            if (Thread.currentThread() != renderThread)
                renderThread.join();
        } catch(InterruptedException e) {
            // do nothing
        }
        
        for (Sync<FrameStatistics> s: queue)
            s.cancel(true);
        queue.clear();
    }

    @Override
    public Future<FrameStatistics> render(List<Action> actions) {
        if (destroyed.get()) // shouldn't happen
            return new CompletedFuture<FrameStatistics>(new FrameStatistics());
        
        Sync<FrameStatistics> sync = new Sync<FrameStatistics>(new RenderCallable(actions));
        queue.add(sync);
        return new FutureSync<FrameStatistics>(sync);
    }
    
    /* Internal classes performing the grunt work */

    private class RenderWorker implements Runnable {
        @Override
        public void run() {
            while(!destroyed.get()) {
                try {
                    Sync<FrameStatistics> sync = queue.take();
                    frameworkLock.readLock().lock();
                    try {
                        sync.run();
                    } finally {
                        frameworkLock.readLock().unlock();
                    }
                } catch(InterruptedException ie) {
                    // do nothing
                }
            }
        }
    }
    
    private class RenderCallable implements Callable<FrameStatistics> {
        private final List<Action> actions;
        
        public RenderCallable(List<Action> actions) {
            this.actions = actions;
        }
        
        @Override
        public FrameStatistics call() throws Exception {
            FrameStatistics stats = new FrameStatistics();
            long now = System.nanoTime();
            
            if (actions != null) {
                // prepare all actions
                Iterator<Action> it = actions.iterator();
                while(it.hasNext()) {
                    Action a = it.next();
                    if (!a.prepare())
                        it.remove();
                }
            }
            
            if (actions != null && !actions.isEmpty()) {
                Context batchOwner = null;
                AbstractSurface lastSurface = null; // might not be batchOwner
                List<Action> batch = new ArrayList<Action>();
                
                AbstractSurface s;
                for (Action a: actions) {
                    s = (AbstractSurface) a.getSurface();
                    if (s == null || s == lastSurface) {
                        // just add it
                        batch.add(a);
                    } else {
                        // surface change, so add the post action for the last surface
                        if (lastSurface != null)
                            batch.add(lastSurface.getPostRenderAction());
                        
                        if (s.getContext() != null && batchOwner != s.getContext()) {
                            // new surface has a different context, so the batch is over
                            if (batch.size() > 0 && batchOwner != null) {
                                doBatch(batchOwner, batch, stats);
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
                
                // render the last batch if needed
                if (batch.size() > 0) {
                    if (lastSurface != null)
                        batch.add(lastSurface.getPostRenderAction());
                    doBatch(batchOwner, batch, stats);
                }
            }
            
            stats.setRenderTime(System.nanoTime() - now);
            return stats;
        }
        
        private void doBatch(Context batchOwner, List<Action> actions, FrameStatistics stats) throws InterruptedException {
            if (destroyed.get())
                return; // end now and don't finish the batch
            
            if (batchOwner == null || batchOwner == resourceManager.getContext())
                resourceManager.runOnResourceThread(new BatchRunner(resourceManager.getContext(), 
                                                                    actions, stats));
            else
                batchOwner.runWithLock(new BatchRunner(batchOwner, actions, stats));
        }
    }
    
    private class BatchRunner implements Runnable {
        private final List<Action> actions;
        private final FrameStatistics stats;
        private final Context context;
        
        public BatchRunner(Context context, List<Action> actions, FrameStatistics stats) {
            this.actions = actions;
            this.stats = stats;
            this.context = context;
        }
        
        @Override
        public void run() {
            Action a = null;
            AbstractSurface lastSurface = null;
            try {
                context.setFrameStatistics(stats);
                context.getRenderer().reset();
                int size = actions.size();
                for (int i = 0; i < size; i++) {
                    if (destroyed.get())
                        break; // silently quit, no need to throw exceptions

                    a = actions.get(i);
                    if (a.prepare()) {
                        lastSurface = (AbstractSurface) a.getSurface();
                        a.perform(context, (i < size - 1 ? actions.get(i + 1) : null));
                    }
                }
            } catch(RuntimeException e) {
                // must invoke the post render action to make sure the surface
                // can get unlocked
                if (lastSurface != null && lastSurface.getPostRenderAction() != a)
                    lastSurface.getPostRenderAction().perform(context, null);
                throw e;
            } finally {
                context.setFrameStatistics(null);
            }
        }
    }
}

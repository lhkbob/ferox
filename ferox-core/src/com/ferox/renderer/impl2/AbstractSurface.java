package com.ferox.renderer.impl2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.Surface;

public abstract class AbstractSurface implements Surface {
    private final AtomicBoolean destroyed;
    private final ReentrantLock lock;
    private final AbstractFramework framework;
    
    public AbstractSurface(AbstractFramework framework) {
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        
        destroyed = new AtomicBoolean(false);
        lock = new ReentrantLock();
        this.framework = framework;
    }
    
    public abstract OpenGLContextAdapter getContext();
    
    public void onSurfaceActivate(OpenGLContextAdapter context, int layer) {
        // Set the viewport of the renderers to match the surface
        FixedFunctionRenderer ffp = context.getFixedFunctionRenderer();
        if (ffp != null)
            ffp.setViewport(0, 0, getWidth(), getHeight());
        
        GlslRenderer glsl = context.getGlslRenderer();
        if (glsl != null)
            glsl.setViewport(0, 0, getWidth(), getHeight());
    }
    
    public void onSurfaceDeactivate(OpenGLContextAdapter context) {
        // Reset the renderers so that the next task sees a clean slate
        // and any locked resources get released
        FixedFunctionRenderer ffp = context.getFixedFunctionRenderer();
        if (ffp != null)
            ffp.reset();
        
        GlslRenderer glsl = context.getGlslRenderer();
        if (glsl != null)
            glsl.reset();
    }
    
    protected abstract void destroyImpl();
    
    // TODO doc that these should not be used to synchronize setters, since
    // a current surface could block a set for a very long time
    public ReentrantLock getLock() {
        return lock;
    }
    
    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            // Since this is just a remove operation, we don't need to wait for
            // the lock on the surface.
            framework.markSurfaceDestroyed(this);
            
            // Check if we already have a lock
            if (lock.isHeldByCurrentThread() && framework.getContextManager().isContextThread()) {
                // If we're locked on a context thread then we need to forcefully unlock
                // it first so that it gets deactivated and its context released
                framework.getContextManager().forceRelease(this);
            }
            
            // Use the ContextManager method to handle negotiating across threads with
            // persistent locks.
            framework.getContextManager().lock(this);
            try {
                destroyImpl();
            } finally {
                // ContextManager.lock() doesn't create a persistent lock so
                // we can use unlock directly on our lock object. 
                lock.unlock();
            }
        }
    }
    
    @Override
    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public Framework getFramework() {
        return framework;
    }
}

package com.ferox.renderer.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.Surface;

/**
 * AbstractSurface is an abstract class implementing Surface. Its primary
 * purpose is to expose additional functionality needed by the components of
 * {@link AbstractFramework} to implement the framework system easily across
 * many adapters for OpenGL.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractSurface implements Surface {
    private final AtomicBoolean destroyed;
    private final ReentrantLock lock;
    private final AbstractFramework framework;

    /**
     * Create a new AbstractSurface that has yet to be destroyed, that is owned
     * by the given Framework.
     * 
     * @param framework The AbstractFramework to be returned by getFramework()
     * @throws NullPointerException if framework is null
     */
    public AbstractSurface(AbstractFramework framework) {
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        
        destroyed = new AtomicBoolean(false);
        lock = new ReentrantLock();
        this.framework = framework;
    }

    /**
     * Return the OpenGLContext that must be current in order to render
     * into this Surface. It can be null to signal that the surface requires any
     * other context to use (such as when a TextureSurface is backed by an FBO).
     * 
     * @return The context of this surface
     */
    public abstract OpenGLContext getContext();

    /**
     * <p>
     * onSurfaceActivate() is a listener method that is invoked by
     * ContextManager when a surface is activated. The provided context is the
     * current context on the calling thread and will not be null. The
     * <tt>layer</tt> argument represents the layer to activate. If the surface
     * does not use layers (such as an OnscreenSurface or 2D TextureSurface) it
     * can be ignored. If it is a cubemap TextureSurface, the represents one of
     * the six faces. If it is a 3D TextureSurface, it represents the depth
     * plane. It can be assumed that the layer argument is valid.
     * </p>
     * <p>
     * This method can be overridden by subclasses to perform more actions. The
     * current implementation activates and sets the viewport on any renderers
     * the context has.
     * </p>
     * 
     * @param context The current context
     * @param layer The layer to activate if the surface is a TextureSurface of
     *            an appropriate target
     */
    public void onSurfaceActivate(OpenGLContext context, int layer) {
        // Set the viewport of the renderers to match the surface
        FixedFunctionRenderer ffp = context.getFixedFunctionRenderer();
        if (ffp != null) {
            if (ffp instanceof AbstractRenderer)
                ((AbstractRenderer) ffp).activate(this, context, framework.getResourceManager());
            ffp.setViewport(0, 0, getWidth(), getHeight());
        }
        
        GlslRenderer glsl = context.getGlslRenderer();
        if (glsl != null) {
            if (glsl instanceof AbstractRenderer)
                ((AbstractRenderer) glsl).activate(this, context, framework.getResourceManager());
            glsl.setViewport(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * onSurfaceDeactivate() is a listener method that is invoked by
     * ContextManager when a surface is deactivated. The provided context is the
     * current context on the calling thread and will not be null. This method
     * can be overridden by subclasses to perform more actions. The current
     * implementation resets any renderers the context has.
     * 
     * @param context The current context
     */
    public void onSurfaceDeactivate(OpenGLContext context) {
        // Reset the renderers so that the next task sees a clean slate
        // and any locked resources get released
        FixedFunctionRenderer ffp = context.getFixedFunctionRenderer();
        if (ffp != null)
            ffp.reset();
        
        GlslRenderer glsl = context.getGlslRenderer();
        if (glsl != null)
            glsl.reset();
    }

    /**
     * Perform the actual destruction of this surface. This will only be called
     * once and the surface's lock will already be held. If the surface has a
     * context, this method is responsible for invoking
     * {@link OpenGLContext#destroy()}.
     */
    protected abstract void destroyImpl();

    /**
     * Return the lock that guards this Surface's context. If the surface does
     * not have a context, this lock guards any other resources used by the
     * surface when it must be activated (such as an FBO). It should not be used
     * to provide synchronization in any mutators because this lock can be held
     * for long periods of time while rendering.
     * 
     * @return The lock used by {@link ContextManager} to maintain thread safety
     *         for contexts and to prevent surfaces from being destroyed while
     *         they are in use
     */
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

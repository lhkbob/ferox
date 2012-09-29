package com.ferox.renderer.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;
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
    private final AbstractFramework framework;

    /**
     * Create a new AbstractSurface that has yet to be destroyed, that is owned
     * by the given Framework.
     * 
     * @param framework The AbstractFramework to be returned by getFramework()
     * @throws NullPointerException if framework is null
     */
    public AbstractSurface(AbstractFramework framework) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        destroyed = new AtomicBoolean(false);
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
     * Perform actions as needed to flush this surface, as required by
     * {@link Context#flush()}.
     * 
     * @param context The current context
     */
    public abstract void flush(OpenGLContext context);

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
        RenderCapabilities caps = context.getRenderCapabilities();
        FixedFunctionRenderer ffp = context.getRendererProvider().getFixedFunctionRenderer(caps);
        if (ffp instanceof AbstractRenderer) {
            ((AbstractRenderer) ffp).activate(this, context, framework.getResourceManager());
        }

        GlslRenderer glsl = context.getRendererProvider().getGlslRenderer(caps);
        if (glsl instanceof AbstractRenderer) {
            ((AbstractRenderer) glsl).activate(this, context, framework.getResourceManager());
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
        RenderCapabilities caps = context.getRenderCapabilities();
        FixedFunctionRenderer ffp = context.getRendererProvider().getFixedFunctionRenderer(caps);
        if (ffp != null) {
            ffp.reset();
        }

        GlslRenderer glsl = context.getRendererProvider().getGlslRenderer(caps);
        if (glsl != null) {
            glsl.reset();
        }
    }

    /**
     * Perform the actual destruction of this surface. This will only be called
     * once and the surface's lock will already be held. If the surface has a
     * context, this method is responsible for invoking
     * {@link OpenGLContext#destroy()}.
     */
    protected abstract void destroyImpl();

    @Override
    public Future<Void> destroy() {
        // First call to destroy handles the destroy operation
        if (destroyed.compareAndSet(false, true)) {
            // Accept this even during shutdown so that surfaces are destroyed
            return framework.getContextManager().invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Must force a release in case this surface was a context provider
                    framework.getContextManager().forceRelease(AbstractSurface.this);
                    destroyImpl();
                    framework.markSurfaceDestroyed(AbstractSurface.this);
                    return null;
                }
            }, true);
        } else {
            // If we've already been destroyed, use a completed future so
            // it's seen as completed
            return new CompletedFuture<Void>(null);
        }
    }

    @Override
    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public AbstractFramework getFramework() {
        return framework;
    }
}

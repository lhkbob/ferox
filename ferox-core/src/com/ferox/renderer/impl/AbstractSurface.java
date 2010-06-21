package com.ferox.renderer.impl;

import java.util.concurrent.locks.ReentrantLock;

import com.ferox.math.Color4f;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Surface;

/**
 * An abstract implementation of Surface that implements the basic methods that
 * are unlikely to require renderer intervention, and provides some additional
 * methods that are useful for AbstractFramework, and the abstract Resource and
 * Render managers.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractSurface implements Surface {
    private static final Color4f DEFAULT_CLEAR_COLOR = new Color4f();

    private final Color4f clearColor;
    private float clearDepth;
    private int clearStencil;
    
    private final Action postRenderAction;
    private final Action preRenderAction;
    
    private boolean renderedOnce;
    private volatile boolean destroyed;
    
    protected final ReentrantLock lock;
    protected final Framework framework;

    public AbstractSurface(Framework framework) {
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        this.framework = framework;
        
        postRenderAction = new PostRenderAction();
        preRenderAction = new PreRenderAction();
        
        renderedOnce = false;
        destroyed = false;

        lock = new ReentrantLock();
        
        clearColor = new Color4f(DEFAULT_CLEAR_COLOR);
        setClearDepth(1f);
        setClearStencil(0);
    }
    
    @Override
    public Framework getFramework() {
        return framework;
    }

    @Override
    public Color4f getClearColor() {
        return clearColor;
    }

    @Override
    public void setClearColor(Color4f color) {
        if (color == null)
            color = new Color4f(DEFAULT_CLEAR_COLOR);
        clearColor.set(color);
    }

    @Override
    public float getClearDepth() {
        return clearDepth;
    }

    @Override
    public void setClearDepth(float depth) {
        if (depth < 0f || depth > 1f)
            throw new IllegalArgumentException("Invalid depth clear value: " + depth);
        clearDepth = depth;
    }

    @Override
    public int getClearStencil() {
        return clearStencil;
    }

    @Override
    public void setClearStencil(int stencil) {
        clearStencil = stencil;
    }
    
    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
    
    /**
     * @return An Action to be invoked before any Actions that rely on this
     *         surface. The RenderManagers should insert this into the
     *         beginning of a batch to be rendered.
     */
    public Action getPreRenderAction() {
        return preRenderAction;
    }

    /**
     * @return An Action to be invoked when all Actions that rely on this
     *         surface have been completed. The RenderManagers should insert
     *         this into the end of a batch to be rendered.
     */
    public Action getPostRenderAction() {
        return postRenderAction;
    }

    /**
     * Destroy the AbstractSurface, this should only be called by the
     * Framework in response to its own {@link Framework#destroy(Surface)}
     * method.
     */
    public void destroy() {
        try {
            lock.lock();
            if (destroyed)
                return;
            
            destroyed = true;
            destroyImpl();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invoked by {@link #destroy()} after acquiring the surface lock and
     * flagging this surface as being destroyed. If necessary, this should also
     * block until the Surface's context is able to be destroyed.
     */
    protected abstract void destroyImpl();

    /**
     * @return The Context associated with this surface, may be null if this
     *         surface relies on an FBO or similar
     */
    public abstract Context getContext();

    /**
     * This method is invoked the first time that the Surface is rendered
     * into.
     */
    protected abstract void init();

    /**
     * Invoked each time just before Actions associated with this Surface
     * are invoked.
     */
    protected abstract void preRender();

    /**
     * Invoked each time just after Actions associated with this Surface
     * are completed.
     * 
     * @param next The next Action to be rendered by the RenderManager
     */
    protected abstract void postRender(Action next);
    
    private class PreRenderAction extends Action {
        public PreRenderAction() {
            super(AbstractSurface.this);
        }

        @Override
        public void perform(Context context, Action next) {
            if (destroyed)
                return;
            
            lock.lock(); // will be unlocked with post render action
            if (!renderedOnce) {
                init();
                renderedOnce = true;
            }
            
            AbstractRenderer r = context.getRenderer();
            r.setSurfaceSize(getWidth(), getHeight());
            
            preRender();
        }
    }
    
    private class PostRenderAction extends Action {
        public PostRenderAction() {
            super(AbstractSurface.this);
        }

        @Override
        public void perform(Context context, Action next) {
            try {
                if (!destroyed)
                    postRender(next);
            } finally {
                // must always unlock this, even if postRender throws
                // an exception
                lock.unlock();
            }
        }
    }
}

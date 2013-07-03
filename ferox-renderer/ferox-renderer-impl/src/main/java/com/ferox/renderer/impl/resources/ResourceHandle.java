package com.ferox.renderer.impl.resources;

import com.ferox.renderer.impl.CompletedFuture;
import com.ferox.renderer.impl.DestructibleManager;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class ResourceHandle implements DestructibleManager.ManagedDestructible {
    // FIXME is it best to use a weak reference here? Or should holding onto a resource
    // reference count as enough to prevent the GC of the entire system?
    // I think that seems reasonable, because they all expose the getFramework() instance.
    // it would be weird if that didn't prevent the GC and someone assumed they could
    // get always safe access to the framework that way
    private final WeakReference<FrameworkImpl> framework;
    private final AtomicBoolean destroyed;

    public ResourceHandle(FrameworkImpl framework) {
        this.framework = new WeakReference<>(framework);
        destroyed = new AtomicBoolean(false);
    }

    public FrameworkImpl getFramework() {
        return framework.get();
    }

    public void destroy(OpenGLContext context) {
        // simple guard to destroy this one time only
        if (destroyed.compareAndSet(false, true)) {
            destroyImpl(context);
        }
    }

    protected abstract void destroyImpl(OpenGLContext context);

    @Override
    public Future<Void> destroy() {
        final FrameworkImpl f = framework.get();
        if (!isDestroyed() && f != null) {
            // we do want destroy tasks accepted during shutdown
            return f.getContextManager().invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    OpenGLContext ctx = f.getContextManager().ensureContext();
                    destroy(ctx);
                    return null;
                }
            }, true);
        } else {
            return new CompletedFuture<>(null);
        }
    }

    @Override
    public boolean isDestroyed() {
        return destroyed.get();
    }
}

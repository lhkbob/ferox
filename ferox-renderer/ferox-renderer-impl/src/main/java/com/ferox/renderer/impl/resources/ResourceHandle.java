package com.ferox.renderer.impl.resources;

import com.ferox.renderer.impl.CompletedFuture;
import com.ferox.renderer.impl.DestructibleManager;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class ResourceHandle implements DestructibleManager.ManagedDestructible {
    private final FrameworkImpl framework;
    private final AtomicBoolean destroyed;

    public ResourceHandle(FrameworkImpl framework) {
        this.framework = framework;
        destroyed = new AtomicBoolean(false);
    }

    public FrameworkImpl getFramework() {
        return framework;
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
        if (!isDestroyed()) {
            // we do want destroy tasks accepted during shutdown
            return framework.getContextManager()
                            .invokeOnContextThread(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    OpenGLContext ctx = framework.getContextManager()
                                                                 .ensureContext();
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

package com.ferox.renderer.impl.resources;

import com.ferox.renderer.Framework;
import com.ferox.renderer.Resource;
import com.ferox.renderer.impl.CompletedFuture;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 *
 */
public abstract class AbstractResource<T extends ResourceHandle> implements Resource {
    private final T handle;

    public AbstractResource(T handle) {
        this.handle = handle;

        FrameworkImpl f = handle.getFramework();
        if (f != null) {
            f.getDestructibleManager().manage(this, handle);
        }
    }

    public T getHandle() {
        return handle;
    }

    @Override
    public Framework getFramework() {
        return handle.getFramework();
    }

    @Override
    public Future<Void> refresh() {
        final FrameworkImpl f = handle.getFramework();
        if (f != null) {
            return f.getContextManager().invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    OpenGLContext ctx = f.getContextManager().ensureContext();
                    refreshImpl(ctx);
                    return null;
                }
            }, false);
        } else {
            return new CompletedFuture<>(null);
        }
    }

    public abstract void refreshImpl(OpenGLContext context);

    @Override
    public Future<Void> destroy() {
        return handle.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return handle.isDestroyed();
    }
}

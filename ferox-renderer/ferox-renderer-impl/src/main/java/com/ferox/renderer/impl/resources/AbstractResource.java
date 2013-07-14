package com.ferox.renderer.impl.resources;

import com.ferox.renderer.Framework;
import com.ferox.renderer.Resource;
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
        return f.getContextManager().invokeOnContextThread(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                OpenGLContext ctx = f.getContextManager().ensureContext();
                f.getResourceFactory().refresh(ctx, AbstractResource.this);
                return null;
            }
        }, false);
    }


    @Override
    public Future<Void> destroy() {
        return handle.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return handle.isDestroyed();
    }
}

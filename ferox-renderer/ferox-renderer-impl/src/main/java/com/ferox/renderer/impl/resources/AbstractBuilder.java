package com.ferox.renderer.impl.resources;

import com.ferox.renderer.Resource;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.builder.Builder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 */
public abstract class AbstractBuilder<T extends Resource, H extends ResourceHandle> implements Builder<T> {
    protected final FrameworkImpl framework;
    private boolean built;

    public AbstractBuilder(FrameworkImpl framework) {
        this.framework = framework;
        built = false;
    }

    @Override
    public T build() {
        if (built) {
            throw new IllegalStateException("Cannot call build() multiple times");
        }

        built = true;
        validate();
        Future<T> resource = framework.getContextManager().invokeOnContextThread(new Callable<T>() {
            @Override
            public T call() throws Exception {
                OpenGLContext ctx = framework.getContextManager().ensureContext();
                H handle = allocate(ctx);
                try {
                    pushToGPU(ctx, handle);
                    T resource = wrap(handle);
                    framework.getDestructibleManager().manage(resource, handle);
                    return resource;
                } catch (Exception e) {
                    handle.destroy(ctx);
                    throw e;
                }
            }
        }, false);

        try {
            return resource.get();
        } catch (InterruptedException e) {
            throw new ResourceException("Interrupted while blocking on resource creation", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ResourceException) {
                throw ((ResourceException) e.getCause());
            } else {
                throw new ResourceException("Unexpected exception while building resource", e.getCause());
            }
        }
    }

    protected abstract void validate();

    protected abstract H allocate(OpenGLContext ctx);

    protected abstract void pushToGPU(OpenGLContext ctx, H handle);

    protected abstract T wrap(H handle);
}
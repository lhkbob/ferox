/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

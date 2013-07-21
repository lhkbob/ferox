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

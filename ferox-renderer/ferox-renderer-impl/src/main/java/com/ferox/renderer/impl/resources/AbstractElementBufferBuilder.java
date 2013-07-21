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

import com.ferox.renderer.DataType;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.builder.ElementBufferBuilder;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.nio.ByteBuffer;

/**
 *
 */
public abstract class AbstractElementBufferBuilder
        extends AbstractBuilder<ElementBuffer, BufferImpl.BufferHandle> implements ElementBufferBuilder {
    protected Object array;
    protected int length;
    protected DataType type;

    protected boolean dynamic;

    public AbstractElementBufferBuilder(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    public ElementBufferBuilder fromUnsigned(int[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_INT;
        return this;
    }

    @Override
    public ElementBufferBuilder fromUnsigned(short[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_SHORT;
        return this;
    }

    @Override
    public ElementBufferBuilder fromUnsigned(byte[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_BYTE;
        return this;
    }

    @Override
    public ElementBufferBuilder dynamic() {
        dynamic = true;
        return this;
    }

    @Override
    protected void validate() {
        if (array == null) {
            throw new ResourceException("Data array must be specified");
        }
    }

    @Override
    protected BufferImpl.BufferHandle allocate(OpenGLContext ctx) {
        if (dynamic) {
            // use an inmemory buffer
            return new BufferImpl.BufferHandle(framework, type, BufferUtil.newBuffer(array));
        } else {
            // get a new vbo id and use that
            return new BufferImpl.BufferHandle(framework, type, generateNewBufferID(ctx));
        }
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, BufferImpl.BufferHandle handle) {
        if (handle.vboID > 0) {
            ctx.bindElementVBO(handle);
            pushBufferData(ctx, type, BufferUtil.newBuffer(array));
        } // else I don't have any more work to do for this type of vbo
    }

    @Override
    protected ElementBuffer wrap(BufferImpl.BufferHandle handle) {
        return new ElementBufferImpl(handle, length, array);
    }

    protected abstract int generateNewBufferID(OpenGLContext ctx);

    protected abstract void pushBufferData(OpenGLContext ctx, DataType type, ByteBuffer buffer);

    private static class ElementBufferImpl extends BufferImpl implements ElementBuffer {
        public ElementBufferImpl(BufferHandle handle, int length, Object dataArray) {
            super(handle, length, dataArray);
        }
    }
}

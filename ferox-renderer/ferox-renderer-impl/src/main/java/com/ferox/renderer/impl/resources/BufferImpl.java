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

import com.ferox.renderer.Buffer;
import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.nio.ByteBuffer;

/**
 *
 */
public abstract class BufferImpl extends AbstractResource<BufferImpl.BufferHandle> implements Buffer {
    private final int length;
    private final Object dataArray;

    public BufferImpl(BufferHandle handle, int length, Object dataArray) {
        super(handle);
        this.length = length;
        this.dataArray = dataArray;
    }

    public Object getDataArray() {
        return dataArray;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public DataType getDataType() {
        return getHandle().type;
    }

    public static class BufferHandle extends ResourceHandle {
        public final int vboID;
        public final DataType type;
        public final ByteBuffer inmemoryBuffer;

        public BufferHandle(FrameworkImpl framework, DataType type, int vboID) {
            super(framework);
            this.vboID = vboID;
            this.type = type;
            inmemoryBuffer = null;
        }

        public BufferHandle(FrameworkImpl framework, DataType type, ByteBuffer inmemoryBuffer) {
            super(framework);
            this.inmemoryBuffer = inmemoryBuffer;
            this.type = type;
            vboID = 0;
        }

        @Override
        protected void destroyImpl(OpenGLContext context) {
            // only one of these if's should be true at a time, but let's play it safe
            if (context.getState().arrayVBO == this) {
                context.bindArrayVBO(null);
            }
            if (context.getState().elementVBO == this) {
                context.bindElementVBO(null);
            }
            // this should be safe if vboID == 0
            getFramework().getResourceFactory().deleteVBO(context, this);
        }
    }
}

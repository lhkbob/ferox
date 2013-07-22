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
package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractVertexBufferBuilder;
import com.ferox.renderer.impl.resources.BufferImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import java.nio.ByteBuffer;

/**
 *
 */
public class JoglVertexBufferBuilder extends AbstractVertexBufferBuilder {
    public JoglVertexBufferBuilder(FrameworkImpl framework) {
        super(framework);
    }

    private static GL getGL(OpenGLContext ctx) {
        return ((JoglContext) ctx).getGLContext().getGL();
    }

    @Override
    protected int generateNewBufferID(OpenGLContext ctx) {
        int[] query = new int[1];
        getGL(ctx).glGenBuffers(1, query, 0);
        return query[0];
    }

    @Override
    protected void pushBufferData(OpenGLContext ctx, DataType type, ByteBuffer buffer) {
        getGL(ctx).glBufferData(GL.GL_ARRAY_BUFFER, buffer.capacity(), buffer, GL2GL3.GL_STATIC_READ);
    }

    public static void refreshVertexBuffer(OpenGLContext ctx, VertexBuffer vbo) {
        BufferImpl.BufferHandle h = ((BufferImpl) vbo).getHandle();
        Object data = ((BufferImpl) vbo).getDataArray();

        if (h.inmemoryBuffer != null) {
            // refill the inmemory buffer, don't need to validate size since that is fixed
            if (vbo.getDataType().getJavaPrimitive().equals(float.class)) {
                h.inmemoryBuffer.clear();
                h.inmemoryBuffer.asFloatBuffer().put((float[]) data);
            } else if (vbo.getDataType().getJavaPrimitive().equals(int.class)) {
                h.inmemoryBuffer.clear();
                h.inmemoryBuffer.asIntBuffer().put((int[]) data);
            } else if (vbo.getDataType().getJavaPrimitive().equals(short.class)) {
                h.inmemoryBuffer.clear();
                h.inmemoryBuffer.asShortBuffer().put((short[]) data);
            } else if (vbo.getDataType().getJavaPrimitive().equals(byte.class)) {
                h.inmemoryBuffer.clear();
                h.inmemoryBuffer.put((byte[]) data);
            }
        } else {
            ctx.bindArrayVBO(h);
            ByteBuffer buffer = BufferUtil.newBuffer(data);
            getGL(ctx).glBufferSubData(GL.GL_ARRAY_BUFFER, 0, buffer.capacity(), buffer);
        }
    }
}

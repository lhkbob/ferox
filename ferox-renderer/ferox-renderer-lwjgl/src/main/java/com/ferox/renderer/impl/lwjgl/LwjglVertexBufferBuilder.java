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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractVertexBufferBuilder;
import com.ferox.renderer.impl.resources.BufferImpl;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 *
 */
public class LwjglVertexBufferBuilder extends AbstractVertexBufferBuilder {
    public LwjglVertexBufferBuilder(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    protected int generateNewBufferID(OpenGLContext ctx) {
        return GL15.glGenBuffers();
    }

    @Override
    protected void pushBufferData(OpenGLContext ctx, DataType type, ByteBuffer buffer) {
        switch (type) {
        case FLOAT:
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer.asFloatBuffer(), GL15.GL_STATIC_READ);
            break;
        case INT:
        case NORMALIZED_INT:
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer.asIntBuffer(), GL15.GL_STATIC_READ);
            break;
        case SHORT:
        case NORMALIZED_SHORT:
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer.asShortBuffer(), GL15.GL_STATIC_READ);
            break;
        case BYTE:
        case NORMALIZED_BYTE:
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_READ);
            break;
        default:
            throw new RuntimeException("Unexpected vertex buffer data type: " + type);
        }
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
            if (vbo.getDataType().getJavaPrimitive().equals(float.class)) {
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, BufferUtil.newFloatBuffer((float[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(int.class)) {
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, BufferUtil.newIntBuffer((int[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(short.class)) {
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, BufferUtil.newShortBuffer((short[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(byte.class)) {
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, BufferUtil.newBuffer((byte[]) data));
            }
        }
    }
}

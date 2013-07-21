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

import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractVertexBufferObjectResourceDriver;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import java.nio.Buffer;

/**
 * JoglVertexBufferObjectResourceDriver is a concrete implementation of a ResourceDriver for
 * VertexBufferObjects and depends on the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class JoglVertexBufferObjectResourceDriver extends AbstractVertexBufferObjectResourceDriver {
    private final ThreadLocal<Integer> arrayVboBinding;
    private final ThreadLocal<Integer> elementVboBinding;

    public JoglVertexBufferObjectResourceDriver() {
        arrayVboBinding = new ThreadLocal<Integer>();
        elementVboBinding = new ThreadLocal<Integer>();
    }

    @Override
    protected void glDeleteBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        getGL(context).glDeleteBuffers(1, new int[] { handle.vboID }, 0);
    }

    @Override
    protected void glBindArrayBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        JoglContext c = (JoglContext) context;
        arrayVboBinding.set(c.getArrayVbo());
        c.bindArrayVbo(getGL(context), handle.vboID);
    }

    @Override
    protected void glBindElementBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        JoglContext c = (JoglContext) context;
        elementVboBinding.set(c.getElementVbo());
        c.bindElementVbo(getGL(context), handle.vboID);
    }

    @Override
    protected void glRestoreArrayBuffer(OpenGLContext context) {
        JoglContext c = (JoglContext) context;
        c.bindArrayVbo(getGL(context), arrayVboBinding.get());
    }

    @Override
    protected void glRestoreElementBuffer(OpenGLContext context) {
        JoglContext c = (JoglContext) context;
        c.bindElementVbo(getGL(context), elementVboBinding.get());
    }

    @Override
    protected void glArrayBufferData(OpenGLContext context, Buffer data, DataType type, int length,
                                     StorageMode mode) {
        int usage = (mode == StorageMode.GPU_DYNAMIC ? GL2ES2.GL_STREAM_DRAW : GL.GL_STATIC_DRAW);
        getGL(context).glBufferData(GL.GL_ARRAY_BUFFER, length * type.getByteCount(), data, usage);
    }

    @Override
    protected void glElementBufferData(OpenGLContext context, Buffer data, DataType type, int length,
                                       StorageMode mode) {
        int usage = (mode == StorageMode.GPU_DYNAMIC ? GL2ES2.GL_STREAM_DRAW : GL.GL_STATIC_DRAW);
        getGL(context).glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, length * type.getByteCount(), data, usage);
    }

    @Override
    protected void glArrayBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset,
                                        int length) {
        int vboOffset = offset * type.getByteCount();
        int vboLength = length * type.getByteCount();
        getGL(context).glBufferSubData(GL.GL_ARRAY_BUFFER, vboOffset, vboLength, data);
    }

    @Override
    protected void glElementBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset,
                                          int length) {
        int vboOffset = offset * type.getByteCount();
        int vboLength = length * type.getByteCount();
        getGL(context).glBufferSubData(GL.GL_ELEMENT_ARRAY_BUFFER, vboOffset, vboLength, data);
    }

    private GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }
}

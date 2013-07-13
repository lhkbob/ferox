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

import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractVertexBufferObjectResourceDriver;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL15;

import java.nio.*;

/**
 * LwjglVertexBufferObjectResourceDriver is a concrete implementation of a ResourceDriver for
 * VertexBufferObjects and depends on the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class LwjglVertexBufferObjectResourceDriver extends AbstractVertexBufferObjectResourceDriver {
    private final ThreadLocal<Integer> arrayVboBinding;
    private final ThreadLocal<Integer> elementVboBinding;

    private boolean useARB;
    private boolean capsChecked;

    public LwjglVertexBufferObjectResourceDriver() {
        arrayVboBinding = new ThreadLocal<Integer>();
        elementVboBinding = new ThreadLocal<Integer>();

        capsChecked = false;
    }

    private void checkCaps(OpenGLContext context) {
        if (!capsChecked) {
            useARB = context.getRenderCapabilities().getVersion() < 1.5f;
            capsChecked = true;
        }
    }

    @Override
    protected void glDeleteBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        checkCaps(context);
        if (useARB) {
            ARBBufferObject.glDeleteBuffersARB(handle.vboID);
        } else {
            GL15.glDeleteBuffers(handle.vboID);
        }
    }

    @Override
    protected void glBindArrayBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        LwjglContext c = (LwjglContext) context;
        arrayVboBinding.set(c.getArrayVbo());
        c.bindArrayVbo(handle.vboID);
    }

    @Override
    protected void glBindElementBuffer(OpenGLContext context, VertexBufferObjectHandle handle) {
        LwjglContext c = (LwjglContext) context;
        elementVboBinding.set(c.getElementVbo());
        c.bindElementVbo(handle.vboID);
    }

    @Override
    protected void glRestoreArrayBuffer(OpenGLContext context) {
        LwjglContext c = (LwjglContext) context;
        c.bindArrayVbo(arrayVboBinding.get());
    }

    @Override
    protected void glRestoreElementBuffer(OpenGLContext context) {
        LwjglContext c = (LwjglContext) context;
        c.bindElementVbo(elementVboBinding.get());
    }

    @Override
    protected void glArrayBufferData(OpenGLContext context, Buffer data, DataType type, int length,
                                     StorageMode mode) {
        checkCaps(context);
        glBufferData(false, data, type, mode);
    }

    @Override
    protected void glElementBufferData(OpenGLContext context, Buffer data, DataType type, int length,
                                       StorageMode mode) {
        checkCaps(context);
        glBufferData(true, data, type, mode);
    }

    private void glBufferData(boolean elementBuffer, Buffer data, DataType type, StorageMode mode) {
        if (useARB) {
            int usage = (mode == StorageMode.GPU_DYNAMIC ? ARBBufferObject.GL_STREAM_DRAW_ARB
                                                         : ARBBufferObject.GL_STATIC_DRAW_ARB);
            int target = (elementBuffer ? ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB
                                        : ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB);

            switch (type) {
            case FLOAT:
                ARBBufferObject.glBufferDataARB(target, (FloatBuffer) data, usage);
                break;
            case BYTE:
                ARBBufferObject.glBufferDataARB(target, (ByteBuffer) data, usage);
                break;
            case INT:
                ARBBufferObject.glBufferDataARB(target, (IntBuffer) data, usage);
                break;
            case SHORT:
                ARBBufferObject.glBufferDataARB(target, (ShortBuffer) data, usage);
                break;
            }
        } else {
            int usage = (mode == StorageMode.GPU_DYNAMIC ? GL15.GL_STREAM_DRAW : GL15.GL_STATIC_DRAW);
            int target = (elementBuffer ? GL15.GL_ELEMENT_ARRAY_BUFFER : GL15.GL_ARRAY_BUFFER);

            switch (type) {
            case FLOAT:
                GL15.glBufferData(target, (FloatBuffer) data, usage);
                break;
            case BYTE:
                GL15.glBufferData(target, (ByteBuffer) data, usage);
                break;
            case INT:
                GL15.glBufferData(target, (IntBuffer) data, usage);
                break;
            case SHORT:
                GL15.glBufferData(target, (ShortBuffer) data, usage);
                break;
            }
        }
    }

    @Override
    protected void glArrayBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset,
                                        int length) {
        checkCaps(context);
        glBufferSubData(false, data, type, offset);
    }

    @Override
    protected void glElementBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset,
                                          int length) {
        checkCaps(context);
        glBufferSubData(true, data, type, offset);
    }

    private void glBufferSubData(boolean elementBuffer, Buffer data, DataType type, int offset) {
        int vboOffset = offset * type.getByteCount();
        if (useARB) {
            int target = (elementBuffer ? ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB
                                        : ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB);

            switch (type) {
            case FLOAT:
                ARBBufferObject.glBufferSubDataARB(target, vboOffset, (FloatBuffer) data);
                break;
            case BYTE:
                ARBBufferObject.glBufferSubDataARB(target, vboOffset, (ByteBuffer) data);
                break;
            case INT:
                ARBBufferObject.glBufferSubDataARB(target, vboOffset, (IntBuffer) data);
                break;
            case SHORT:
                ARBBufferObject.glBufferSubDataARB(target, vboOffset, (ShortBuffer) data);
                break;
            }
        } else {
            int target = (elementBuffer ? GL15.GL_ELEMENT_ARRAY_BUFFER : GL15.GL_ARRAY_BUFFER);

            switch (type) {
            case FLOAT:
                GL15.glBufferSubData(target, vboOffset, (FloatBuffer) data);
                break;
            case BYTE:
                GL15.glBufferSubData(target, vboOffset, (ByteBuffer) data);
                break;
            case INT:
                GL15.glBufferSubData(target, vboOffset, (IntBuffer) data);
                break;
            case SHORT:
                GL15.glBufferSubData(target, vboOffset, (ShortBuffer) data);
                break;
            }
        }
    }
}

package com.ferox.renderer.impl.jogl;

import java.nio.Buffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractVertexBufferObjectResourceDriver;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * JoglVertexBufferObjectResourceDriver is a concrete implementation of a
 * ResourceDriver for VertexBufferObjects and depends on the JOGL OpenGL
 * binding.
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
        getGL(context).glDeleteBuffers(1, new int[] {handle.vboID}, 0);
    }

    @Override
    protected void glBindArrayBuffer(OpenGLContext context,
                                     VertexBufferObjectHandle handle) {
        JoglContext c = (JoglContext) context;
        arrayVboBinding.set(c.getArrayVbo());
        c.bindArrayVbo(getGL(context), handle.vboID);
    }

    @Override
    protected void glBindElementBuffer(OpenGLContext context,
                                       VertexBufferObjectHandle handle) {
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
    protected void glArrayBufferData(OpenGLContext context, Buffer data, DataType type,
                                     int length, StorageMode mode) {
        int usage = (mode == StorageMode.GPU_DYNAMIC ? GL2ES2.GL_STREAM_DRAW : GL.GL_STATIC_DRAW);
        getGL(context).glBufferData(GL.GL_ARRAY_BUFFER, length * type.getByteCount(),
                                    data, usage);
    }

    @Override
    protected void glElementBufferData(OpenGLContext context, Buffer data, DataType type,
                                       int length, StorageMode mode) {
        int usage = (mode == StorageMode.GPU_DYNAMIC ? GL2ES2.GL_STREAM_DRAW : GL.GL_STATIC_DRAW);
        getGL(context).glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,
                                    length * type.getByteCount(), data, usage);
    }

    @Override
    protected void glArrayBufferSubData(OpenGLContext context, Buffer data,
                                        DataType type, int offset, int length) {
        int vboOffset = offset * type.getByteCount();
        int vboLength = length * type.getByteCount();
        getGL(context).glBufferSubData(GL.GL_ARRAY_BUFFER, vboOffset, vboLength, data);
    }

    @Override
    protected void glElementBufferSubData(OpenGLContext context, Buffer data,
                                          DataType type, int offset, int length) {
        int vboOffset = offset * type.getByteCount();
        int vboLength = length * type.getByteCount();
        getGL(context).glBufferSubData(GL.GL_ELEMENT_ARRAY_BUFFER, vboOffset, vboLength,
                                       data);
    }

    private GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }
}

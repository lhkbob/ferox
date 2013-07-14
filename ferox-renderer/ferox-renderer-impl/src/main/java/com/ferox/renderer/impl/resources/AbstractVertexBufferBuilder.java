package com.ferox.renderer.impl.resources;

import com.ferox.renderer.DataType;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.builder.VertexBufferBuilder;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

/**
 *
 */
public abstract class AbstractVertexBufferBuilder
        extends AbstractBuilder<VertexBuffer, BufferImpl.BufferHandle> implements VertexBufferBuilder {
    protected Object array;
    protected int length;
    protected DataType type;

    protected boolean dynamic;

    public AbstractVertexBufferBuilder(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    public VertexBufferBuilder from(float[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.FLOAT;
        return this;
    }

    @Override
    public VertexBufferBuilder fromNormalized(int[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.NORMALIZED_INT;
        return this;
    }

    @Override
    public VertexBufferBuilder fromNormalized(short[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.NORMALIZED_SHORT;
        return this;
    }

    @Override
    public VertexBufferBuilder fromNormalized(byte[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.NORMALIZED_BYTE;
        return this;
    }

    @Override
    public VertexBufferBuilder from(int[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.INT;
        return this;
    }

    @Override
    public VertexBufferBuilder from(short[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.SHORT;
        return this;
    }

    @Override
    public VertexBufferBuilder from(byte[] data) {
        if (data == null) {
            throw new NullPointerException("Data array cannot be null");
        }
        array = data;
        length = data.length;
        type = DataType.BYTE;
        return this;
    }

    @Override
    public VertexBufferBuilder dynamic() {
        dynamic = true;
        return this;
    }

    @Override
    protected void validate() {
        if (!framework.getCapabilities().getVertexBufferSupport()) {
            dynamic = true;
        }
        if (array == null) {
            throw new ResourceException("Data array must be specified");
        }
    }

    @Override
    protected BufferImpl.BufferHandle allocate(OpenGLContext ctx) {
        if (dynamic) {
            // use an inmemory buffer
            return new BufferImpl.BufferHandle(framework, BufferUtil.newBuffer(array));
        } else {
            // get a new vbo id and use that
            return new BufferImpl.BufferHandle(framework, generateNewBufferID(ctx));
        }
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, BufferImpl.BufferHandle handle) {
        if (handle.vboID > 0) {
            ctx.bindArrayVBO(handle);
            pushBufferData(ctx, BufferUtil.newBuffer(array));
        } // else I don't have any more work to do for this type of vbo
    }

    @Override
    protected VertexBuffer wrap(BufferImpl.BufferHandle handle) {
        return new VertexBufferImpl(handle, type, length, array);
    }

    protected abstract int generateNewBufferID(OpenGLContext ctx);

    protected abstract void pushBufferData(OpenGLContext ctx, java.nio.Buffer buffer);

    private static class VertexBufferImpl extends BufferImpl implements VertexBuffer {

        public VertexBufferImpl(BufferHandle handle, DataType type, int length, Object dataArray) {
            super(handle, type, length, dataArray);
        }
    }
}

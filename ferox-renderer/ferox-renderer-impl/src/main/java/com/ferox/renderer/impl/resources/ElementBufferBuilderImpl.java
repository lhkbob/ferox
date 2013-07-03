package com.ferox.renderer.impl.resources;

import com.ferox.renderer.DataType;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.builder.ElementBufferBuilder;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

/**
 *
 */
public abstract class ElementBufferBuilderImpl
        extends AbstractBuilder<ElementBuffer, BufferImpl.BufferHandle>
        implements ElementBufferBuilder {
    protected Object array;
    protected int length;
    protected DataType type;

    protected boolean dynamic;

    public ElementBufferBuilderImpl(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    public ElementBufferBuilder fromUnsigned(int[] data) {
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_NORMALIZED_INT;
        return this;
    }

    @Override
    public ElementBufferBuilder fromUnsigned(short[] data) {
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_NORMALIZED_SHORT;
        return this;
    }

    @Override
    public ElementBufferBuilder fromUnsigned(byte[] data) {
        array = data;
        length = data.length;
        type = DataType.UNSIGNED_NORMALIZED_BYTE;
        return this;
    }

    @Override
    public ElementBufferBuilder dynamic() {
        dynamic = true;
        return this;
    }

    @Override
    protected void validate() {
        if (!framework.getCapabilities().getVertexBufferSupport()) {
            throw new ResourceException(
                    "VertexBuffers aren't supported by current hardware");
        }
        if (array == null) {
            throw new ResourceException("Data array must not be specified");
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
            ctx.bindElementVBO(handle);
            pushBufferData(ctx, BufferUtil.newBuffer(array));
        } // else I don't have any more work to do for this type of vbo
    }

    protected abstract int generateNewBufferID(OpenGLContext ctx);

    protected abstract void pushBufferData(OpenGLContext ctx, java.nio.Buffer buffer);
}

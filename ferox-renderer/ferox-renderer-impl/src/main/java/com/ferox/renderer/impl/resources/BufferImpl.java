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
            if (vboID > 0) {
                // only one of these if's should be true at a time, but let's play it safe
                if (context.getCurrentSharedState().arrayVBO == this) {
                    context.bindArrayVBO(null);
                }
                if (context.getCurrentSharedState().elementVBO == this) {
                    context.bindElementVBO(null);
                }
                getFramework().getResourceFactory().deleteVBO(context, this);
            }
        }
    }
}

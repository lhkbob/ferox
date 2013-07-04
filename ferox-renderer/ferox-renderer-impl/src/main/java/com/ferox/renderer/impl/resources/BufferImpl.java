package com.ferox.renderer.impl.resources;

import com.ferox.renderer.DataType;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

/**
 *
 */
public class BufferImpl extends AbstractResource<BufferImpl.BufferHandle>
        implements VertexBuffer, ElementBuffer {
    private final int length;
    private final DataType type;
    private final Object dataArray;

    public BufferImpl(BufferHandle handle, DataType type, int length, Object dataArray) {
        super(handle);
        this.type = type;
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
        return type;
    }

    public static class BufferHandle extends ResourceHandle {
        public final int vboID;
        public final java.nio.Buffer inmemoryBuffer;

        public BufferHandle(FrameworkImpl framework, int vboID) {
            super(framework);
            this.vboID = vboID;
            inmemoryBuffer = null;
        }

        public BufferHandle(FrameworkImpl framework, java.nio.Buffer inmemoryBuffer) {
            super(framework);
            this.inmemoryBuffer = inmemoryBuffer;
            vboID = 0;
        }

        @Override
        protected void destroyImpl(OpenGLContext context) {
            if (vboID > 0) {
                // only one of these if's should be true at a time, but let's play it safe
                if (context.getArrayVBO() == this) {
                    context.bindArrayVBO(null);
                }
                if (context.getElementVBO() == this) {
                    context.bindElementVBO(null);
                }
                getFramework().getResourceFactory().deleteVBO(context, this);
            }
        }
    }
}

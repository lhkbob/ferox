package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractElementBufferBuilder;
import com.ferox.renderer.impl.resources.BufferImpl;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 *
 */
public class LwjglElementBufferBuilder extends AbstractElementBufferBuilder {
    public LwjglElementBufferBuilder(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    protected int generateNewBufferID(OpenGLContext ctx) {
        return GL15.glGenBuffers();
    }

    @Override
    protected void pushBufferData(OpenGLContext ctx, DataType type, ByteBuffer buffer) {
        switch (type) {
        case UNSIGNED_INT:
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer.asIntBuffer(), GL15.GL_STATIC_READ);
            break;
        case UNSIGNED_SHORT:
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer.asShortBuffer(), GL15.GL_STATIC_READ);
            break;
        case UNSIGNED_BYTE:
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_READ);
            break;
        default:
            throw new RuntimeException("Unexpected element buffer data type: " + type);
        }
    }

    public static void refreshElementBuffer(OpenGLContext ctx, ElementBuffer vbo) {
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
            ctx.bindElementVBO(h);
            if (vbo.getDataType().getJavaPrimitive().equals(float.class)) {
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0,
                                     BufferUtil.newFloatBuffer((float[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(int.class)) {
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, BufferUtil.newIntBuffer((int[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(short.class)) {
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0,
                                     BufferUtil.newShortBuffer((short[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(byte.class)) {
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, BufferUtil.newBuffer((byte[]) data));
            }
        }
    }
}

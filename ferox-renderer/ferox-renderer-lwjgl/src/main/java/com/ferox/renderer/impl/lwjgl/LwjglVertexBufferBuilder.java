package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractVertexBufferBuilder;
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
}

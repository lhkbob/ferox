package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractElementBufferBuilder;
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
}

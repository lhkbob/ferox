package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.*;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 *
 */
public class LwjglResourceFactory implements ResourceFactory {
    @Override
    public VertexBufferBuilder newVertexBufferBuilder(FrameworkImpl framework) {
        return new LwjglVertexBufferBuilder(framework);
    }

    @Override
    public ElementBufferBuilder newElementBufferBuilder(FrameworkImpl framework) {
        return new LwjglElementBufferBuilder(framework);
    }

    @Override
    public ShaderBuilder newShaderBuilder(FrameworkImpl framework) {
        return new LwjglShaderBuilder(framework);
    }

    @Override
    public Texture2DBuilder newTexture2DBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public Texture2DArrayBuilder newTexture2DArrayBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public Texture1DBuilder newTexture1DBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public Texture1DArrayBuilder newTexture1DArrayBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public Texture3DBuilder newTexture3DBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public TextureCubeMapBuilder newTextureCubeMapBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public DepthMap2DBuilder newDepthMap2DBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public DepthCubeMapBuilder newDepthCubeMapBuilder(FrameworkImpl framework) {
        return null;
    }

    @Override
    public void deleteVBO(OpenGLContext context, BufferImpl.BufferHandle vbo) {
        if (vbo.vboID > 0) {
            GL15.glDeleteBuffers(vbo.vboID);
        }
    }

    @Override
    public void deleteShader(OpenGLContext context, ShaderImpl.ShaderHandle shader) {
        GL20.glDeleteShader(shader.vertexShaderID);
        GL20.glDeleteShader(shader.fragmentShaderID);
        if (shader.geometryShaderID > 0) {
            GL20.glDeleteShader(shader.geometryShaderID);
        }
        GL20.glDeleteProgram(shader.programID);
    }

    @Override
    public void deleteTexture(OpenGLContext context, TextureImpl.TextureHandle texture) {
    }

    @Override
    public void refresh(OpenGLContext context, AbstractResource<?> resource) {
        if (resource instanceof BufferImpl) {
            refreshVBO(context, (BufferImpl) resource);
        }

        // otherwise the resource does not support refreshing its state
    }

    private void refreshVBO(OpenGLContext ctx, BufferImpl vbo) {
        BufferImpl.BufferHandle h = vbo.getHandle();
        Object data = vbo.getDataArray();
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
            int target;
            if (vbo instanceof VertexBuffer) {
                ctx.bindArrayVBO(h);
                target = GL15.GL_ARRAY_BUFFER;
            } else {
                ctx.bindElementVBO(h);
                target = GL15.GL_ELEMENT_ARRAY_BUFFER;
            }

            if (vbo.getDataType().getJavaPrimitive().equals(float.class)) {
                GL15.glBufferSubData(target, 0, BufferUtil.newFloatBuffer((float[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(int.class)) {
                GL15.glBufferSubData(target, 0, BufferUtil.newIntBuffer((int[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(short.class)) {
                GL15.glBufferSubData(target, 0, BufferUtil.newShortBuffer((short[]) data));
            } else if (vbo.getDataType().getJavaPrimitive().equals(byte.class)) {
                GL15.glBufferSubData(target, 0, BufferUtil.newBuffer((byte[]) data));
            }
        }
    }
}

package com.ferox.renderer.impl.jogl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexBufferObject.StorageMode;

public class JoglGlslRenderer extends AbstractGlslRenderer {
    private boolean initialized;
    private EnumSet<Target> supportedTargets;

    public JoglGlslRenderer(JoglRendererDelegate delegate) {
        super(delegate);
    }

    // FIXME: must understand how to implement binding to a render target,
    //   especially across shader versions
    @Override
    public void bindRenderTarget(String fragmentVariable, int target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context,
                         ResourceManager manager) {
        super.activate(surface, context, manager);

        if (!initialized) {
            // detect caps
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            supportedTargets = caps.getSupportedTextureTargets();

            initialized = true;
        }
    }

    @Override
    protected void glUseProgram(GlslShaderHandle shader) {
        int pid = (shader == null ? 0 : shader.programID);
        ((JoglContext) context).bindGlslProgram(getGL(), pid);
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3,
                                    float v4) {
        switch (rowCount) {
        case 1:
            getGL().glVertexAttrib1f(attr, v1);
            break;
        case 2:
            getGL().glVertexAttrib2f(attr, v1, v2);
            break;
        case 3:
            getGL().glVertexAttrib3f(attr, v1, v2, v3);
            break;
        case 4:
            getGL().glVertexAttrib4f(attr, v1, v2, v3, v4);
            break;
        }
    }

    @Override
    protected void glUniform(Uniform u, FloatBuffer values, int count) {
        switch (u.uniform.getType()) {
        case FLOAT:
            getGL().glUniform1fv(u.index, count, values);
            break;
        case FLOAT_VEC2:
            getGL().glUniform2fv(u.index, count, values);
            break;
        case FLOAT_VEC3:
            getGL().glUniform3fv(u.index, count, values);
            break;
        case FLOAT_VEC4:
            getGL().glUniform4fv(u.index, count, values);
            break;
        case FLOAT_MAT2:
            getGL().glUniformMatrix2fv(u.index, count, false, values);
            break;
        case FLOAT_MAT3:
            getGL().glUniformMatrix3fv(u.index, count, false, values);
            break;
        case FLOAT_MAT4:
            getGL().glUniformMatrix4fv(u.index, count, false, values);
            break;
        }
    }

    @Override
    protected void glUniform(Uniform u, IntBuffer values, int count) {
        switch (u.uniform.getType()) {
        case BOOL:
        case INT:
        case SHADOW_MAP:
        case TEXTURE_1D:
        case TEXTURE_2D:
        case TEXTURE_3D:
        case TEXTURE_CUBEMAP:
            getGL().glUniform1iv(u.index, count, values);
            break;
        case INT_VEC2:
            getGL().glUniform2iv(u.index, count, values);
            break;
        case INT_VEC3:
            getGL().glUniform3iv(u.index, count, values);
            break;
        case INT_VEC4:
            getGL().glUniform4iv(u.index, count, values);
            break;
        }
    }

    @Override
    protected void glBindTexture(int tex, Target target, TextureHandle handle) {
        if (supportedTargets.contains(target)) {
            JoglContext ctx = (JoglContext) context;
            GL2GL3 gl = getGL();
            ctx.setActiveTexture(gl, tex);
            ctx.bindTexture(gl, Utils.getGLTextureTarget(target),
                            (handle == null ? 0 : handle.texID));
        }
    }

    @Override
    protected void glEnableAttribute(int attr, boolean enable) {
        if (enable) {
            getGL().glEnableVertexAttribArray(attr);
        } else {
            getGL().glDisableVertexAttribArray(attr);
        }
    }

    @Override
    protected void glBindArrayVbo(VertexBufferObjectHandle h) {
        JoglContext ctx = (JoglContext) context;
        GL2GL3 gl = getGL();

        if (h != null) {
            if (h.mode != StorageMode.IN_MEMORY) {
                // Must bind the VBO
                ctx.bindArrayVbo(gl, h.vboID);
            } else {
                // Must unbind any old VBO, will grab the in-memory buffer during render call
                ctx.bindArrayVbo(gl, 0);
            }
        } else {
            // Must unbind the vbo
            ctx.bindArrayVbo(gl, 0);
        }
    }

    @Override
    protected void glAttributePointer(int attr, VertexBufferObjectHandle h, int offset,
                                      int stride, int elementSize) {
        int strideBytes = (elementSize + stride) * h.dataType.getByteCount();

        if (h.mode == StorageMode.IN_MEMORY) {
            h.inmemoryBuffer.clear().position(offset);
            getGL().glVertexAttribPointer(attr, elementSize, GL.GL_FLOAT, false,
                                          strideBytes, h.inmemoryBuffer);
        } else {
            int vboOffset = offset * h.dataType.getByteCount();
            getGL().glVertexAttribPointer(attr, elementSize, GL.GL_FLOAT, false,
                                          strideBytes, vboOffset);
        }
    }

    private GL2GL3 getGL() {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }
}

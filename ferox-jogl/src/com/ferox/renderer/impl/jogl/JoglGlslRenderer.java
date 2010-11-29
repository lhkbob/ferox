package com.ferox.renderer.impl.jogl;

import java.util.EnumSet;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.resource.GlslShaderHandle;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.resource.Geometry;
import com.ferox.resource.Texture.Target;

public class JoglGlslRenderer extends AbstractGlslRenderer {
    private final EnumSet<Target> supportedTargets;

    // context cache for faster gl lookups
    private JoglContext context;
    
    public JoglGlslRenderer(AbstractFramework framework) {
        super(new JoglRendererDelegate(framework.getCapabilities()), framework);
        supportedTargets = framework.getCapabilities().getSupportedTextureTargets();
    }
    
    private GL2GL3 getGL() {
        // we cache the context the first time we need it,
        // a renderer will not be passed around amongst contexts
        if (context == null)
            context = JoglContext.getCurrent();
        return context.getGL();
    }

    @Override
    protected void glUseProgram(GlslShaderHandle shader) {
        int pid = (shader != null ? shader.getId() : 0);
        getGL().glUseProgram(pid);
    }

    @Override
    protected void glUniform(Uniform u, float[] values, int count) {
        GL2GL3 gl = getGL();
        
        switch(u.uniform.getType()) {
        case FLOAT:
            if (count == 1)
                gl.glUniform1f(u.index, values[0]);
            else
                gl.glUniform1fv(u.index, count, values, 0);
            break;
        case FLOAT_VEC2:
            if (count == 1)
                gl.glUniform2f(u.index, values[0], values[1]);
            else
                gl.glUniform2fv(u.index, count, values, 0);
            break;
        case FLOAT_VEC3:
            if (count == 1)
                gl.glUniform3f(u.index, values[0], values[1], values[2]);
            else
                gl.glUniform3fv(u.index, count, values, 0);
            break;
        case FLOAT_VEC4:
            if (count == 1)
                gl.glUniform4f(u.index, values[0], values[1], values[2], values[3]);
            else
                gl.glUniform4fv(u.index, count, values, 0);
            break;
        case FLOAT_MAT2:
            gl.glUniformMatrix2fv(u.index, count, false, values, count);
            break;
        case FLOAT_MAT3:
            gl.glUniformMatrix3fv(u.index, count, false, values, count);
            break;
        case FLOAT_MAT4:
            gl.glUniformMatrix4fv(u.index, count, false, values, count);
            break;
        }
    }

    @Override
    protected void glUniform(Uniform u, int[] values, int count) {
        GL2GL3 gl = getGL();
        
        switch(u.uniform.getType()) {
        case INT: case BOOL: case SHADOW_MAP: case TEXTURE_1D:
        case TEXTURE_2D: case TEXTURE_3D: case TEXTURE_CUBEMAP:
            if (count == 1)
                gl.glUniform1i(u.index, values[0]);
            else
                gl.glUniform1iv(u.index, count, values, 0);
            break;
        case INT_VEC2:
            if (count == 1)
                gl.glUniform2i(u.index, values[0], values[1]);
            else
                gl.glUniform2iv(u.index, count, values, 0);
            break;
        case INT_VEC3:
            if (count == 1)
                gl.glUniform3i(u.index, values[0], values[1], values[2]);
            else
                gl.glUniform3iv(u.index, count, values, 0);
            break;
        case INT_VEC4:
            if (count == 1)
                gl.glUniform4i(u.index, values[0], values[1], values[2], values[3]);
            else
                gl.glUniform4iv(u.index, count, values, 0);
            break;
        }
    }

    @Override
    protected void glBindTexture(int tex, Target target, TextureHandle handle) {
        if (supportedTargets.contains(target)) {
            GL2GL3 gl = getGL();
            BoundObjectState record = context.getRecord();
            
            int glTarget = Utils.getGLTextureTarget(target);
            if (record.getActiveTexture() != tex)
                record.setActiveTexture(gl, tex);
            
            // the BoundObjectState takes care of the same id for us,
            // and unbinding old textures on a different target
            if (handle == null) {
                record.bindTexture(gl, glTarget, 0);
            } else {
                record.bindTexture(gl, glTarget, handle.getId());
            }
        }
    }

    @Override
    public int render(Geometry g) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    protected void init() {
        // do nothing
    }
}

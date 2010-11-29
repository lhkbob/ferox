package com.ferox.renderer.impl.jogl;

import java.util.EnumSet;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.resource.GeometryHandle;
import com.ferox.renderer.impl.resource.GlslShaderHandle;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.renderer.impl.resource.VertexArray;
import com.ferox.resource.Geometry;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.GlslShader;
import com.ferox.resource.Texture.Target;

public class JoglGlslRenderer extends AbstractGlslRenderer {
    private final EnumSet<Target> supportedTargets;

    private GeometryHandle lastGeometry;
    private final VertexArray[] boundAttributes;
    
    // context cache for faster gl lookups
    private JoglContext context;
    
    public JoglGlslRenderer(AbstractFramework framework) {
        super(new JoglRendererDelegate(framework.getCapabilities()), framework);
        
        supportedTargets = framework.getCapabilities().getSupportedTextureTargets();
        boundAttributes = new VertexArray[framework.getCapabilities().getMaxVertexAttributes()];
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
        GL2GL3 gl = getGL();
        context.getRecord().bindGlslProgram(gl, pid);
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
    public int render(Geometry geom) {
        ResourceHandle handle = resourceManager.getHandle(geom);
        if (handle != null) {
            super.render(geom);
            return renderImpl((GeometryHandle) handle);
        } else
            return 0;
    }
    
    private int renderImpl(GeometryHandle geom) {
        GL2GL3 gl = getGL();
        BoundObjectState record = context.getRecord();
        
        boolean useVbos = geom.compile != CompileType.NONE;
        int vertexCount = -1;
        
        if (geom != lastGeometry) {
            if (!useVbos) {
                record.bindArrayVbo(gl, 0);
                record.bindElementVbo(gl, 0);
            } else {
                record.bindArrayVbo(gl, geom.arrayVbo);
                record.bindElementVbo(gl, geom.elementVbo);
            }
            
            // update the attribute bindings before rendering the geometry
            AttributeBinding binding;
            VertexArray va;
            for (int i = 0; i < attributeBindings.length; i++) {
                binding = attributeBindings[i];
                for (int j = 0; j < binding.columnNames.length; j++) {
                    if (binding.columnNames[j] != null) {
                        // array binding
                        if (!bindAttributeArray(gl, binding, j, geom, useVbos))
                            return 0; // type mismatch between geometry and generic attr
                        
                        if (vertexCount < 0) {
                            // compute vertex count from first vertex array
                            va = boundAttributes[binding.attr.index + j];
                            vertexCount = (useVbos ? va.vboLen / (4 * va.elementSize) : va.buffer.capacity() / va.elementSize);
                        }
                    } else if (binding.columnValuesValid[j]) {
                        // direct generic attribute call
                        bindAttributeDirect(gl, binding, j);
                    } else {
                        // not all attributes are specified, so there's an error
                        return 0;
                    }
                }
            }
            
            if (vertexCount < 0)
                vertexCount = 1; // no arrays in use, means only direct attributes are in use
        }
        
        int glPolyType = Utils.getGLPolygonConnectivity(geom.polyType);
        if (useVbos)
            gl.glDrawRangeElements(glPolyType, geom.minIndex, geom.maxIndex, 
                                   geom.indexCount, GL2GL3.GL_UNSIGNED_INT, 0);
        else
            gl.glDrawRangeElements(glPolyType, geom.minIndex, geom.maxIndex, 
                                   geom.indexCount, GL2GL3.GL_UNSIGNED_INT, geom.indices.rewind());
        
        lastGeometry = geom;
        context.getFrameStatistics().add(1, vertexCount, geom.polyCount);
        return geom.polyCount;
    }
    
    private boolean bindAttributeArray(GL2GL3 gl, AttributeBinding binding, int column, GeometryHandle geom, boolean vbo) {
        VertexArray va = getVertexArray(geom, binding.attr.name);
        if (va == null || va.elementSize != binding.attr.type.getRows())
            return false;
        
        int attrIndex = binding.attr.index + column;
        if (boundAttributes[attrIndex] != va) {
            if (boundAttributes[attrIndex] == null)
                gl.glEnableVertexAttribArray(attrIndex);
            
            if (vbo)
                gl.glVertexAttribPointer(attrIndex, va.elementSize, GL.GL_FLOAT, true, 0, va.offset);
            else
                gl.glVertexAttribPointer(attrIndex, va.elementSize, GL.GL_FLOAT, true, 0, va.buffer.rewind());
        }
        return true;
    }
    
    private void bindAttributeDirect(GL2GL3 gl, AttributeBinding binding, int column) {
        int attrIndex = binding.attr.index + column;
        if (boundAttributes[attrIndex] != null) {
            gl.glDisableVertexAttribArray(attrIndex);
            boundAttributes[attrIndex] = null;
        }
        
        switch(binding.attr.type) {
        case FLOAT:
            gl.glVertexAttrib1f(attrIndex, binding.columnValues[column]);
            break;
        case FLOAT_VEC2: case FLOAT_MAT2:
            gl.glVertexAttrib2f(attrIndex, binding.columnValues[column * 2], binding.columnValues[column * 2 + 1]);
            break;
        case FLOAT_VEC3: case FLOAT_MAT3:
            gl.glVertexAttrib3f(attrIndex, binding.columnValues[column * 3],
                                binding.columnValues[column * 3 + 1], binding.columnValues[column * 3 + 2]);
            break;
        case FLOAT_VEC4: case FLOAT_MAT4:
            gl.glVertexAttrib4f(attrIndex, binding.columnValues[column * 4], 
                                binding.columnValues[column * 4 + 1], binding.columnValues[column * 4 + 2], 
                                binding.columnValues[column * 4 + 3]);
        }
    }
    
    private VertexArray getVertexArray(GeometryHandle handle, String name) {
        if (name == null)
            return null;
        
        VertexArray arr;
        int len = handle.compiledPointers.size();
        for (int i = 0; i < len; i++) {
            arr = handle.compiledPointers.get(i);
            if (arr.name.equals(name))
                return arr;
        }
        // couldn't find a match
        return null;
    }
    
    private void resetGeometry(GL2GL3 gl) {
        for (int i = 0; i < boundAttributes.length; i++) {
            if (boundAttributes[i] != null) {
                gl.glDisableVertexAttribArray(i);
                boundAttributes[i] = null;
            }
        }
        
        lastGeometry = null;
    }
    
    @Override
    public void setShader(GlslShader shader) {
        super.setShader(shader);
        resetGeometry(getGL());
    }
    
    @Override
    protected void onBindAttribute(String glslAttrName) {
        resetGeometry(getGL());
    }
    
    @Override
    public void reset() {
        super.reset();
        
        GL2GL3 gl = getGL();
        BoundObjectState record = context.getRecord();
        
        // unbind vbos
        record.bindArrayVbo(gl, 0);
        record.bindElementVbo(gl, 0);
        
        resetGeometry(gl);
        
        // clear context cache
        context = null;
    }
    
    @Override
    protected void init() {
        // do nothing
    }
}

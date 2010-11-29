package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.resource.AbstractGlslShaderResourceDriver;
import com.ferox.renderer.impl.resource.GlslShaderHandle;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Uniform;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

public class JoglGlslShaderDriver extends AbstractGlslShaderResourceDriver {
    public JoglGlslShaderDriver(RenderCapabilities caps) {
        super(caps);
    }

    @Override
    protected int glCreateProgram() {
        return JoglContext.getCurrent().getGL().glCreateProgram();
    }

    @Override
    protected int glCreateShader(ShaderType type) {
        return JoglContext.getCurrent().getGL().glCreateShader(Utils.getGLShaderType(type));
    }

    @Override
    protected String glCompileShader(int shaderId, String[] code) {
        GL2GL3 gl = JoglContext.getCurrent().getGL();

        // set the source code for the shader
        int[] lens = new int[code.length];
        for (int i = 0; i < code.length; i++)
            lens[i] = code[i].length();
        gl.glShaderSource(shaderId, code.length, code, lens, 0);
        
        // compile the shader
        gl.glCompileShader(shaderId);
        
        // query compile status and possibly read log
        int[] status = new int[1];
        gl.glGetShaderiv(shaderId, GL2GL3.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL.GL_TRUE)
            return null; // everything compiled successfully, no log
        
        // compile failed, read the log and return it
        gl.glGetShaderiv(shaderId, GL2GL3.GL_INFO_LOG_LENGTH, status, 0);
        int maxLogLength = status[0];
        byte[] log = new byte[maxLogLength];
        gl.glGetShaderInfoLog(shaderId, maxLogLength, status, 0, log, 0);
        
        return new String(log, 0, status[0]);
    }

    @Override
    protected void glDeleteShader(int id) {
        JoglContext.getCurrent().getGL().glDeleteShader(id);
    }

    @Override
    protected void glDeleteProgram(int id) {
        JoglContext.getCurrent().getGL().glDeleteProgram(id);
    }

    @Override
    protected void glAttachShader(int programId, int shaderId) {
        JoglContext.getCurrent().getGL().glAttachShader(programId, shaderId);
    }

    @Override
    protected void glDetachShader(int programId, int shaderId) {
        JoglContext.getCurrent().getGL().glDetachShader(programId, shaderId);
    }

    @Override
    protected String glCompileProgram(int programId) {
        // link the program
        GL2GL3 gl = JoglContext.getCurrent().getGL();
        gl.glLinkProgram(programId);
        
        // check link status
        int[] query = new int[1];
        gl.glGetProgramiv(programId, GL2GL3.GL_LINK_STATUS, query, 0);
        if (query[0] == GL.GL_TRUE)
            return null; // program linked successfully
        
        // link failed, read the log and return it
        gl.glGetProgramiv(programId, GL2GL3.GL_INFO_LOG_LENGTH, query, 0);
        int maxLogLength = query[0];
        byte[] log = new byte[maxLogLength];
        gl.glGetProgramInfoLog(programId, maxLogLength, query, 0, log, 0);
        
        return new String(log, 0, query[0]);
    }

    @Override
    protected void updateAttributes(GlslShaderHandle handle) {
        GL2GL3 gl = JoglContext.getCurrent().getGL();
        
        int[] query = new int[1];
        gl.glGetProgramiv(handle.getId(), GL2GL3.GL_ACTIVE_ATTRIBUTES, query, 0);
        int numAttrs = query[0];
        
        gl.glGetProgramiv(handle.getId(), GL2GL3.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, query, 0);
        int maxAttributeNameLength = query[0];
        byte[] name = new byte[maxAttributeNameLength];
        
        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numAttrs; i++) {
            // read uniform properties
            gl.glGetActiveAttrib(handle.getId(), i, maxAttributeNameLength, nameLen, 0, len, 0, type, 0, name, 0);
            String attrName = new String(name, 0, nameLen[0]);
            int index = gl.glGetAttribLocation(handle.getId(), attrName);
            Attribute a = new Attribute(attrName, Utils.getAttributeType(type[0]), index);
            
            handle.attributes.put(attrName, a);
        } 
    }

    @Override
    protected void updateUniforms(GlslShaderHandle handle) {
        GL2GL3 gl = JoglContext.getCurrent().getGL();
        
        int[] query = new int[1];
        gl.glGetProgramiv(handle.getId(), GL2GL3.GL_ACTIVE_UNIFORMS, query, 0);
        int numUniforms = query[0];
        
        gl.glGetProgramiv(handle.getId(), GL2GL3.GL_ACTIVE_UNIFORM_MAX_LENGTH, query, 0);
        int maxUniformNameLength = query[0];
        byte[] name = new byte[maxUniformNameLength];
        
        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numUniforms; i++) {
            // read uniform properties
            gl.glGetActiveUniform(handle.getId(), i, maxUniformNameLength, nameLen, 0, len, 0, type, 0, name, 0);
            String uniformName = new String(name, 0, nameLen[0]);
            GlslUniform u = new GlslUniform(uniformName, Utils.getUniformType(type[0]), len[0]);
            
            // get uniform location
            int location = gl.glGetUniformLocation(handle.getId(), uniformName);
            
            handle.uniforms.put(uniformName, new Uniform(u, location));
        }
    }
}

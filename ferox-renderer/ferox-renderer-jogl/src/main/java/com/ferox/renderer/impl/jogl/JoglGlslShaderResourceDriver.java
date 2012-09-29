package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractGlslShaderResourceDriver;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

/**
 * JoglGlslShaderResourceDriver is a concrete ResourceDriver that handles
 * GlslShaders using the JOGL OpenGL binding.
 * 
 * @author Michael Ludwig
 */
public class JoglGlslShaderResourceDriver extends AbstractGlslShaderResourceDriver {
    @Override
    protected int glCreateProgram(OpenGLContext context) {
        return getGL(context).glCreateProgram();
    }

    @Override
    protected int glCreateShader(OpenGLContext context, ShaderType type) {
        int shaderType = Utils.getGLShaderType(type);
        return getGL(context).glCreateShader(shaderType);
    }

    @Override
    protected String glCompileShader(OpenGLContext context, int shaderId, String code) {
        GL2GL3 gl = getGL(context);

        // set the source code for the shader
        gl.glShaderSource(shaderId, 1, new String[] { code }, new int[] { code.length() }, 0);
        // compile the shader
        gl.glCompileShader(shaderId);

        // query compile status and possibly read log
        int[] status = new int[1];
        gl.glGetShaderiv(shaderId, GL2ES2.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL.GL_TRUE)
        {
            return null; // everything compiled successfully, no log
        }

        // compile failed, read the log and return it
        gl.glGetShaderiv(shaderId, GL2ES2.GL_INFO_LOG_LENGTH, status, 0);
        int maxLogLength = status[0];
        if (maxLogLength > 0) {
            byte[] log = new byte[maxLogLength];
            gl.glGetShaderInfoLog(shaderId, maxLogLength, status, 0, log, 0);

            return new String(log, 0, status[0]);
        } else {
            return "unknown compilation error";
        }
    }

    @Override
    protected void glDeleteShader(OpenGLContext context, int id) {
        getGL(context).glDeleteShader(id);
    }

    @Override
    protected void glDeleteProgram(OpenGLContext context, int id) {
        getGL(context).glDeleteProgram(id);
    }

    @Override
    protected void glAttachShader(OpenGLContext context, int programId, int shaderId) {
        getGL(context).glAttachShader(programId, shaderId);
    }

    @Override
    protected void glDetachShader(OpenGLContext context, int programId, int shaderId) {
        getGL(context).glDetachShader(programId, shaderId);
    }

    @Override
    protected String glLinkProgram(OpenGLContext context, int programId) {
        // link the program
        GL2GL3 gl = getGL(context);
        gl.glLinkProgram(programId);

        // check link status
        int[] query = new int[1];
        gl.glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, query, 0);
        if (query[0] == GL.GL_TRUE)
        {
            return null; // program linked successfully
        }

        // link failed, read the log and return it
        gl.glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, query, 0);
        int maxLogLength = query[0];
        if (maxLogLength > 0) {
            byte[] log = new byte[maxLogLength];
            gl.glGetProgramInfoLog(programId, maxLogLength, query, 0, log, 0);

            return new String(log, 0, query[0]);
        } else {
            return "unknown link error";
        }
    }

    @Override
    protected void updateAttributes(OpenGLContext context, GlslShaderHandle handle) {
        GL2GL3 gl = getGL(context);

        int[] query = new int[1];
        gl.glGetProgramiv(handle.programID, GL2ES2.GL_ACTIVE_ATTRIBUTES, query, 0);
        int numAttrs = query[0];

        gl.glGetProgramiv(handle.programID, GL2ES2.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, query, 0);
        int maxAttributeNameLength = query[0];
        byte[] name = new byte[maxAttributeNameLength];

        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numAttrs; i++) {
            // read uniform properties
            gl.glGetActiveAttrib(handle.programID, i, maxAttributeNameLength, nameLen, 0, len, 0, type, 0, name, 0);
            String attrName = new String(name, 0, nameLen[0]);
            int index = gl.glGetAttribLocation(handle.programID, attrName);
            Attribute a = new Attribute(attrName, Utils.getAttributeType(type[0]), index);

            handle.attributes.put(attrName, a);
        }
    }

    @Override
    protected void updateUniforms(OpenGLContext context, GlslShaderHandle handle) {
        GL2GL3 gl = getGL(context);

        int[] query = new int[1];
        gl.glGetProgramiv(handle.programID, GL2ES2.GL_ACTIVE_UNIFORMS, query, 0);
        int numUniforms = query[0];

        gl.glGetProgramiv(handle.programID, GL2ES2.GL_ACTIVE_UNIFORM_MAX_LENGTH, query, 0);
        int maxUniformNameLength = query[0];
        byte[] name = new byte[maxUniformNameLength];

        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numUniforms; i++) {
            // read uniform properties
            gl.glGetActiveUniform(handle.programID, i, maxUniformNameLength, nameLen, 0, len, 0, type, 0, name, 0);
            String uniformName = new String(name, 0, nameLen[0]);
            GlslUniform u = new GlslUniform(uniformName, Utils.getUniformType(type[0]), len[0]);

            // get uniform location
            int location = gl.glGetUniformLocation(handle.programID, uniformName);

            handle.uniforms.put(uniformName, new Uniform(u, location));
        }
    }

    private GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }
}

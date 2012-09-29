package com.ferox.renderer.impl.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractGlslShaderResourceDriver;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

/**
 * LwjglGlslShaderResourceDriver is a concrete ResourceDriver that handles
 * GlslShaders using the JOGL OpenGL binding.
 * 
 * @author Michael Ludwig
 */
public class LwjglGlslShaderResourceDriver extends AbstractGlslShaderResourceDriver {
    @Override
    protected int glCreateProgram(OpenGLContext context) {
        return GL20.glCreateProgram();
    }

    @Override
    protected int glCreateShader(OpenGLContext context, ShaderType type) {
        int shaderType = Utils.getGLShaderType(type);
        return GL20.glCreateShader(shaderType);
    }

    @Override
    protected String glCompileShader(OpenGLContext context, int shaderId, String code) {
        // set the source code for the shader
        GL20.glShaderSource(shaderId, code);
        // compile the shader
        GL20.glCompileShader(shaderId);

        // query compile status and possibly read log
        if (GL20.glGetShader(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE)
        {
            return null; // everything compiled successfully, no log
        }

        // compile failed, read the log and return it
        int maxLogLength = GL20.glGetShader(shaderId, GL20.GL_INFO_LOG_LENGTH);
        if (maxLogLength > 0) {
            return GL20.glGetShaderInfoLog(shaderId, maxLogLength);
        } else {
            return "unknown compilation error";
        }
    }

    @Override
    protected void glDeleteShader(OpenGLContext context, int id) {
        GL20.glDeleteShader(id);
    }

    @Override
    protected void glDeleteProgram(OpenGLContext context, int id) {
        GL20.glDeleteProgram(id);
    }

    @Override
    protected void glAttachShader(OpenGLContext context, int programId, int shaderId) {
        GL20.glAttachShader(programId, shaderId);
    }

    @Override
    protected void glDetachShader(OpenGLContext context, int programId, int shaderId) {
        GL20.glDetachShader(programId, shaderId);
    }

    @Override
    protected String glLinkProgram(OpenGLContext context, int programId) {
        // link the program
        GL20.glLinkProgram(programId);

        // check link status
        if (GL20.glGetProgram(programId, GL20.GL_LINK_STATUS) == GL11.GL_TRUE)
        {
            return null; // program linked successfully
        }

        // link failed, read the log and return it
        int maxLogLength = GL20.glGetProgram(programId, GL20.GL_INFO_LOG_LENGTH);
        if (maxLogLength > 0) {
            return GL20.glGetProgramInfoLog(programId, maxLogLength);
        } else {
            return "unknown link error";
        }
    }

    @Override
    protected void updateAttributes(OpenGLContext context, GlslShaderHandle handle) {
        int numAttrs = GL20.glGetProgram(handle.programID, GL20.GL_ACTIVE_ATTRIBUTES);
        int maxAttributeNameLength = GL20.glGetProgram(handle.programID, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH);
        ByteBuffer name = BufferUtil.newByteBuffer(maxAttributeNameLength);

        IntBuffer nameLen = BufferUtil.newIntBuffer(1);
        IntBuffer len = BufferUtil.newIntBuffer(1);
        IntBuffer type = BufferUtil.newIntBuffer(1);
        for (int i = 0; i < numAttrs; i++) {
            // read uniform properties
            GL20.glGetActiveAttrib(handle.programID, i, nameLen, len, type, name);
            byte[] bs = new byte[nameLen.get(0)];
            name.get(bs, 0, bs.length);
            String attrName = new String(bs);

            int index = GL20.glGetAttribLocation(handle.programID, attrName);
            Attribute a = new Attribute(attrName, Utils.getAttributeType(type.get(0)), index);

            handle.attributes.put(attrName, a);
        }
    }

    @Override
    protected void updateUniforms(OpenGLContext context, GlslShaderHandle handle) {
        int numUniforms = GL20.glGetProgram(handle.programID, GL20.GL_ACTIVE_UNIFORMS);
        int maxUniformNameLength = GL20.glGetProgram(handle.programID, GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH);
        ByteBuffer name = BufferUtil.newByteBuffer(maxUniformNameLength);

        IntBuffer nameLen = BufferUtil.newIntBuffer(1);
        IntBuffer len = BufferUtil.newIntBuffer(1);
        IntBuffer type = BufferUtil.newIntBuffer(1);
        for (int i = 0; i < numUniforms; i++) {
            // read uniform properties
            GL20.glGetActiveUniform(handle.programID, i, nameLen, len, type, name);
            byte[] bs = new byte[nameLen.get(0)];
            name.get(bs, 0, bs.length).position(0);
            String uniformName = new String(bs);

            GlslUniform u = new GlslUniform(uniformName, Utils.getUniformType(type.get(0)), len.get(0));

            // get uniform location
            int location = GL20.glGetUniformLocation(handle.programID, uniformName);
            handle.uniforms.put(uniformName, new Uniform(u, location));
        }
    }
}

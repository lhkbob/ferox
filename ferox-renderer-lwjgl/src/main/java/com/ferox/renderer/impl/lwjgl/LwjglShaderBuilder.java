/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DataType;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractShaderBuilder;
import com.ferox.renderer.impl.resources.ShaderImpl;
import org.lwjgl.opengl.EXTGeometryShader4;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LwjglShaderBuilder extends AbstractShaderBuilder {
    public LwjglShaderBuilder(FrameworkImpl framework) {
        super(framework);
    }

    @Override
    protected int createNewProgram(OpenGLContext context) {
        return GL20.glCreateProgram();
    }

    @Override
    protected int createNewVertexShader(OpenGLContext context) {
        return GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
    }

    @Override
    protected int createNewFragmentShader(OpenGLContext context) {
        return GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
    }

    @Override
    protected int createNewGeometryShader(OpenGLContext context) {
        return GL20.glCreateShader(EXTGeometryShader4.GL_GEOMETRY_SHADER_EXT);
    }

    @Override
    protected void compileShader(OpenGLContext context, int shaderID, String code) {
        GL20.glShaderSource(shaderID, code);
        GL20.glCompileShader(shaderID);

        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE) {
            return;
        }

        // otherwise compilation failure
        int maxLogLength = GL20.glGetShaderi(shaderID, GL20.GL_INFO_LOG_LENGTH);
        if (maxLogLength > 0) {
            throw new ResourceException(
                    "shader unit failed to compile: " + GL20.glGetShaderInfoLog(shaderID, maxLogLength));
        } else {
            throw new ResourceException("shader unit failed to compile without providing info log");
        }
    }

    @Override
    protected void attachShader(OpenGLContext context, int programID, int shaderID) {
        GL20.glAttachShader(programID, shaderID);
    }

    @Override
    protected void linkProgram(OpenGLContext context, int programID) {
        // link the program
        GL20.glLinkProgram(programID);

        // check link status
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_TRUE) {
            return;
        }

        // link failed, read the log and return it
        int maxLogLength = GL20.glGetProgrami(programID, GL20.GL_INFO_LOG_LENGTH);
        if (maxLogLength > 0) {
            throw new ResourceException(
                    "program failed to link: " + GL20.glGetProgramInfoLog(programID, maxLogLength));
        } else {
            throw new ResourceException("program failed to link without providing info log");
        }
    }

    @Override
    protected List<ShaderImpl.UniformImpl> getUniforms(OpenGLContext context,
                                                       ShaderImpl.ShaderHandle handle) {
        List<ShaderImpl.UniformImpl> uniforms = new ArrayList<>();

        int programID = handle.programID;
        int numUniforms = GL20.glGetProgrami(programID, GL20.GL_ACTIVE_UNIFORMS);
        int maxUniformNameLength = GL20.glGetProgrami(programID, GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH);
        ByteBuffer name = BufferUtil.newByteBuffer(DataType.BYTE, maxUniformNameLength);

        IntBuffer nameLen = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        IntBuffer len = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        IntBuffer type = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        for (int i = 0; i < numUniforms; i++) {
            // read uniform properties
            GL20.glGetActiveUniform(programID, i, nameLen, len, type, name);
            byte[] bs = new byte[nameLen.get(0)];
            name.get(bs, 0, bs.length).position(0);
            String uniformName = new String(bs);
            int location = GL20.glGetUniformLocation(programID, uniformName);

            ShaderImpl.UniformImpl u = new ShaderImpl.UniformImpl(handle, Utils.getVariableType(type.get(0)),
                                                                  len.get(0), uniformName, location);
            uniforms.add(u);
        }

        return uniforms;
    }

    @Override
    protected List<ShaderImpl.AttributeImpl> getAttributes(OpenGLContext context,
                                                           ShaderImpl.ShaderHandle handle) {
        List<ShaderImpl.AttributeImpl> attributes = new ArrayList<>();

        int programID = handle.programID;
        int numAttrs = GL20.glGetProgrami(programID, GL20.GL_ACTIVE_ATTRIBUTES);
        int maxAttributeNameLength = GL20.glGetProgrami(programID, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH);
        ByteBuffer name = BufferUtil.newByteBuffer(DataType.BYTE, maxAttributeNameLength);

        IntBuffer nameLen = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        IntBuffer len = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        IntBuffer type = BufferUtil.newByteBuffer(DataType.INT, 1).asIntBuffer();
        for (int i = 0; i < numAttrs; i++) {
            // read uniform properties
            GL20.glGetActiveAttrib(programID, i, nameLen, len, type, name);
            byte[] bs = new byte[nameLen.get(0)];
            name.get(bs, 0, bs.length);
            String attrName = new String(bs);

            int index = GL20.glGetAttribLocation(programID, attrName);
            ShaderImpl.AttributeImpl a = new ShaderImpl.AttributeImpl(handle,
                                                                      Utils.getVariableType(type.get(0)),
                                                                      attrName, index);
            attributes.add(a);
        }

        return attributes;
    }

    @Override
    protected void bindFragmentLocation(OpenGLContext context, int programID, String variable, int buffer) {
        GL30.glBindFragDataLocation(programID, buffer, variable);
    }

    @Override
    protected int getFragmentLocation(OpenGLContext context, int programID, String variable) {
        return GL30.glGetFragDataLocation(programID, variable);
    }
}

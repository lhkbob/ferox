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
package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.ResourceException;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractShaderBuilder;
import com.ferox.renderer.impl.resources.ShaderImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class JoglShaderBuilder extends AbstractShaderBuilder {
    public JoglShaderBuilder(FrameworkImpl framework) {
        super(framework);
    }

    private static GL2GL3 getGL(OpenGLContext ctx) {
        return ((JoglContext) ctx).getGLContext().getGL().getGL2GL3();
    }

    @Override
    protected int createNewProgram(OpenGLContext context) {
        return getGL(context).glCreateProgram();
    }

    @Override
    protected int createNewVertexShader(OpenGLContext context) {
        return getGL(context).glCreateShader(GL2GL3.GL_VERTEX_SHADER);
    }

    @Override
    protected int createNewFragmentShader(OpenGLContext context) {
        return getGL(context).glCreateShader(GL2GL3.GL_FRAGMENT_SHADER);
    }

    @Override
    protected int createNewGeometryShader(OpenGLContext context) {
        return getGL(context).glCreateShader(GL3.GL_GEOMETRY_SHADER);
    }

    @Override
    protected void compileShader(OpenGLContext context, int shaderID, String code) {
        getGL(context).glShaderSource(shaderID, 1, new String[] { code }, new int[] { code.length() }, 0);
        getGL(context).glCompileShader(shaderID);

        int[] query = new int[1];
        getGL(context).glGetShaderiv(shaderID, GL2GL3.GL_COMPILE_STATUS, query, 0);
        if (query[0] == GL.GL_TRUE) {
            return;
        }

        // otherwise compilation failure
        getGL(context).glGetShaderiv(shaderID, GL2GL3.GL_INFO_LOG_LENGTH, query, 0);
        int maxLogLength = query[0];
        if (maxLogLength > 0) {
            byte[] log = new byte[maxLogLength];
            getGL(context).glGetShaderInfoLog(shaderID, maxLogLength, query, 0, log, 0);

            String msg = new String(log, 0, query[0]);
            throw new ResourceException("shader unit failed to compile: " + msg);
        } else {
            throw new ResourceException("shader unit failed to compile without providing info log");
        }
    }

    @Override
    protected void attachShader(OpenGLContext context, int programID, int shaderID) {
        getGL(context).glAttachShader(programID, shaderID);
    }

    @Override
    protected void linkProgram(OpenGLContext context, int programID) {
        // link the program
        getGL(context).glLinkProgram(programID);

        // check link status
        int[] query = new int[1];
        getGL(context).glGetProgramiv(programID, GL2GL3.GL_LINK_STATUS, query, 0);
        if (query[0] == GL.GL_TRUE) {
            return;
        }

        // link failed, read the log and return it
        getGL(context).glGetProgramiv(programID, GL2GL3.GL_INFO_LOG_LENGTH, query, 0);
        int maxLogLength = query[0];
        if (maxLogLength > 0) {
            byte[] log = new byte[maxLogLength];
            getGL(context).glGetProgramInfoLog(programID, maxLogLength, query, 0, log, 0);

            String msg = new String(log, 0, query[0]);
            throw new ResourceException("program failed to link: " + msg);
        } else {
            throw new ResourceException("program failed to link without providing info log");
        }
    }

    @Override
    protected List<ShaderImpl.UniformImpl> getUniforms(OpenGLContext context,
                                                       ShaderImpl.ShaderHandle handle) {
        List<ShaderImpl.UniformImpl> uniforms = new ArrayList<>();

        int programID = handle.programID;
        int[] query = new int[1];
        getGL(context).glGetProgramiv(programID, GL2GL3.GL_ACTIVE_UNIFORMS, query, 0);
        int numUniforms = query[0];

        getGL(context).glGetProgramiv(programID, GL2GL3.GL_ACTIVE_UNIFORM_MAX_LENGTH, query, 0);
        int maxUniformNameLength = query[0];
        byte[] name = new byte[maxUniformNameLength];

        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numUniforms; i++) {
            // read uniform properties
            getGL(context)
                    .glGetActiveUniform(programID, i, maxUniformNameLength, nameLen, 0, len, 0, type, 0, name,
                                        0);
            String uniformName = new String(name, 0, nameLen[0]);

            // get uniform location
            int location = getGL(context).glGetUniformLocation(programID, uniformName);

            ShaderImpl.UniformImpl u = new ShaderImpl.UniformImpl(handle, Utils.getVariableType(type[0]),
                                                                  len[0], uniformName, location);
            uniforms.add(u);
        }

        return uniforms;
    }

    @Override
    protected List<ShaderImpl.AttributeImpl> getAttributes(OpenGLContext context,
                                                           ShaderImpl.ShaderHandle handle) {
        List<ShaderImpl.AttributeImpl> attributes = new ArrayList<>();

        int programID = handle.programID;
        int[] query = new int[1];
        getGL(context).glGetProgramiv(programID, GL2GL3.GL_ACTIVE_ATTRIBUTES, query, 0);
        int numAttrs = query[0];

        getGL(context).glGetProgramiv(programID, GL2GL3.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, query, 0);
        int maxAttributeNameLength = query[0];
        byte[] name = new byte[maxAttributeNameLength];

        int[] nameLen = new int[1];
        int[] len = new int[1];
        int[] type = new int[1];
        for (int i = 0; i < numAttrs; i++) {
            // read uniform properties
            getGL(context)
                    .glGetActiveAttrib(programID, i, maxAttributeNameLength, nameLen, 0, len, 0, type, 0,
                                       name, 0);
            String attrName = new String(name, 0, nameLen[0]);

            int index = getGL(context).glGetAttribLocation(programID, attrName);
            ShaderImpl.AttributeImpl a = new ShaderImpl.AttributeImpl(handle, Utils.getVariableType(type[0]),
                                                                      attrName, index);
            attributes.add(a);
        }

        return attributes;
    }

    @Override
    protected void bindFragmentLocation(OpenGLContext context, int programID, String variable, int buffer) {
        getGL(context).glBindFragDataLocation(programID, buffer, variable);
    }

    @Override
    protected int getFragmentLocation(OpenGLContext context, int programID, String variable) {
        return getGL(context).glGetFragDataLocation(programID, variable);
    }
}
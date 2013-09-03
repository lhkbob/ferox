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
package com.ferox.renderer.impl.resources;

import com.ferox.renderer.ResourceException;
import com.ferox.renderer.Shader;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class AbstractShaderBuilder extends AbstractBuilder<Shader, ShaderImpl.ShaderHandle>
        implements ShaderBuilder {
    public static enum ShaderType {
        VERTEX,
        FRAGMENT,
        GEOMETRY
    }

    private static final Pattern VERSION = Pattern.compile("#version (\\d+).*");

    private String vertexCode;
    private String fragmentCode;
    private String geometryCode;

    private final Map<String, Integer> mappedBuffers;

    private int detectedShaderVersion; // cached in validate()
    private List<ShaderImpl.UniformImpl> detectedUniforms; // cached in pushToGPU()
    private List<ShaderImpl.AttributeImpl> detectedAttributes; // cached in pushToGPU()
    private Map<String, Integer> detectedBufferMapping; // cached in pushToGPU()

    public AbstractShaderBuilder(FrameworkImpl framework) {
        super(framework);
        mappedBuffers = new HashMap<>();
    }

    @Override
    public ShaderBuilder withVertexShader(String code) {
        if (code == null) {
            throw new NullPointerException("Vertex shader code cannot be null");
        }
        vertexCode = code;
        return this;
    }

    @Override
    public ShaderBuilder withFragmentShader(String code) {
        if (code == null) {
            throw new NullPointerException("Fragment shader code cannot be null");
        }
        fragmentCode = code;
        return this;
    }

    @Override
    public ShaderBuilder withGeometryShader(String code) {
        geometryCode = code;
        return this;
    }

    @Override
    public ShaderBuilder bindColorBuffer(String variableName, int buffer) {
        if (variableName == null) {
            throw new NullPointerException("Name cannot be null");
        }
        if (variableName.equals("gl_FragColor")) {
            throw new IllegalArgumentException(
                    "Cannot specify color buffer for gl_FragColor, it is implicit");
        } else if (variableName.startsWith("gl_FragData[")) {
            throw new IllegalArgumentException(
                    "Cannot specify color buffer for gl_FragData[n], it is implicit");
        }
        mappedBuffers.put(variableName, buffer);
        return this;
    }

    @Override
    public ShaderBuilder requestBinding(String... variableNames) {
        for (String name : variableNames) {
            bindColorBuffer(name, -1);
        }
        return this;
    }

    @Override
    protected void validate() {
        if (vertexCode == null) {
            throw new ResourceException("Vertex shader code must be provided");
        }
        if (fragmentCode == null) {
            throw new ResourceException("Fragment shader code must be provided");
        }

        int vertexVersion = extractShaderVersion(vertexCode);
        int fragmentVersion = extractShaderVersion(fragmentCode);
        if (fragmentVersion != vertexVersion) {
            throw new ResourceException("GLSL versions inconsistent between vertex (" + vertexVersion +
                                        ") and fragment (" + fragmentVersion + ") shaders");
        }

        if (geometryCode != null) {
            if (!framework.getCapabilities().hasGeometryShaderSupport()) {
                throw new ResourceException("Geometry shaders are not supported on current hardware");
            }

            int geometryVersion = extractShaderVersion(geometryCode);
            if (geometryVersion != vertexVersion) {
                throw new ResourceException("GLSL versions inconsistent between vertex (" + vertexVersion +
                                            ") and geometry (" + geometryVersion + ") shaders");
            }

            // FIXME is this the right version? is this even a valid check to perform?
            if (geometryVersion < 140) {
                throw new ResourceException(
                        "GLSL version detected is below minimum required for geometry shader");
            }
        }

        // the only validation performed on buffer mappings at this point is that
        // the reserved output names gl_FragColor and gl_FragData[n] are not specified
        Set<Integer> assignedBuffers = new HashSet<>();
        for (String output : mappedBuffers.keySet()) {
            Integer buffer = mappedBuffers.get(output);
            if (buffer >= framework.getCapabilities().getMaxColorBuffers()) {
                throw new ResourceException("Current hardware cannot support a color buffer = " + buffer);
            }
            if (buffer >= 0 && assignedBuffers.contains(buffer)) {
                throw new ResourceException("Multiple output variables mapped to the same color buffer");
            }
            assignedBuffers.add(buffer);
        }

        detectedShaderVersion = vertexVersion;
    }

    @Override
    protected ShaderImpl.ShaderHandle allocate(OpenGLContext ctx) {
        int geometryID = (geometryCode == null ? 0 : createNewShader(ctx, ShaderType.GEOMETRY));
        return new ShaderImpl.ShaderHandle(framework, createNewProgram(ctx),
                                           createNewShader(ctx, ShaderType.VERTEX),
                                           createNewShader(ctx, ShaderType.FRAGMENT), geometryID);
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, ShaderImpl.ShaderHandle handle) {
        compileShader(ctx, ShaderType.VERTEX, handle.vertexShaderID, cleanSourceCode(vertexCode));
        compileShader(ctx, ShaderType.FRAGMENT, handle.fragmentShaderID, cleanSourceCode(fragmentCode));
        if (geometryCode != null) {
            compileShader(ctx, ShaderType.GEOMETRY, handle.geometryShaderID, cleanSourceCode(geometryCode));
        }

        attachShader(ctx, handle.programID, handle.vertexShaderID);
        attachShader(ctx, handle.programID, handle.fragmentShaderID);
        if (geometryCode != null) {
            attachShader(ctx, handle.programID, handle.geometryShaderID);
        }

        // bind explicit varying outs before we link
        for (Map.Entry<String, Integer> binding : mappedBuffers.entrySet()) {
            if (binding.getValue() >= 0) {
                bindFragmentLocation(ctx, handle.programID, binding.getKey(), binding.getValue());
            }
        }

        linkProgram(ctx, handle.programID);

        // query linked program state
        detectedUniforms = getUniforms(ctx, handle);
        Collections.sort(detectedUniforms, indexSorter);
        detectedAttributes = getAttributes(ctx, handle);
        Collections.sort(detectedAttributes, indexSorter);

        detectedBufferMapping = new HashMap<>();
        for (String variable : mappedBuffers.keySet()) {
            int location = getFragmentLocation(ctx, handle.programID, variable);
            if (location >= 0) {
                detectedBufferMapping.put(variable, location);
            }
        }
        // add default variables to the map as well
        detectedBufferMapping.put("gl_FragColor", 0);
        for (int i = 0; i < framework.getCapabilities().getMaxColorBuffers(); i++) {
            detectedBufferMapping.put("gl_FragData[" + i + "]", i);
        }
    }

    @Override
    protected Shader wrap(ShaderImpl.ShaderHandle handle) {
        handle.uniforms = detectedUniforms;
        handle.attributes = detectedAttributes;
        return new ShaderImpl(handle, detectedShaderVersion, detectedUniforms, detectedAttributes,
                              detectedBufferMapping);
    }

    protected abstract int createNewProgram(OpenGLContext context);

    protected abstract int createNewShader(OpenGLContext context, ShaderType type);

    protected abstract void compileShader(OpenGLContext context, ShaderType type, int shaderID, String code);

    protected abstract void attachShader(OpenGLContext context, int programID, int shaderID);

    protected abstract void linkProgram(OpenGLContext context, int programID);

    protected abstract List<ShaderImpl.UniformImpl> getUniforms(OpenGLContext context,
                                                                ShaderImpl.ShaderHandle handle);

    protected abstract List<ShaderImpl.AttributeImpl> getAttributes(OpenGLContext context,
                                                                    ShaderImpl.ShaderHandle handle);

    protected abstract void bindFragmentLocation(OpenGLContext context, int programID, String variable,
                                                 int buffer);

    protected abstract int getFragmentLocation(OpenGLContext context, int programID, String variable);

    private int extractShaderVersion(String code) {
        // we trim to remove blank lines preceeding #version, and we make it lower case
        // to support different capitalizations of #version
        String firstLine = code.trim().substring(0, code.indexOf('\n')).toLowerCase();
        Matcher m = VERSION.matcher(firstLine);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        } else {
            // if the version pattern doesn't match, we assume its 110
            // if there was actually a syntax error, etc. the compiler should detect that
            return 110;
        }
    }

    private String cleanSourceCode(String code) {
        return code.replaceAll("\r\n", "\n");
    }

    private static final Comparator<Shader.Variable> indexSorter = new Comparator<Shader.Variable>() {
        @Override
        public int compare(Shader.Variable o1, Shader.Variable o2) {
            return o1.getIndex() - o2.getIndex();
        }
    };
}

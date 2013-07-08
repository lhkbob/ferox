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
public abstract class AbstractShaderBuilder
        extends AbstractBuilder<Shader, ShaderImpl.ShaderHandle>
        implements ShaderBuilder {
    private static final Pattern VERSION = Pattern.compile("#VERSION (\\d+)[\\s]+.*");

    private String vertexCode;
    private String fragmentCode;
    private String geometryCode;

    private final Map<String, Integer> mappedBuffers;

    private int detectedShaderVersion; // cached in validate()
    private List<Shader.Uniform> detectedUniforms; // cached in pushToGPU()
    private List<Shader.Attribute> detectedAttributes; // cached in pushToGPU()
    private Map<String, Integer> detectedBufferMapping; // cached in pushToGPU()

    public AbstractShaderBuilder(FrameworkImpl framework) {
        super(framework);
        mappedBuffers = new HashMap<>();
    }

    @Override
    public ShaderBuilder withVertexShader(String code) {
        vertexCode = code;
        return this;
    }

    @Override
    public ShaderBuilder withFragmentShader(String code) {
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
            throw new ResourceException(
                    "GLSL versions inconsistent between vertex (" + vertexVersion +
                    ") and fragment (" + fragmentVersion + ") shaders");
        }

        if (geometryCode != null) {
            if (!framework.getCapabilities().hasGeometryShaderSupport()) {
                throw new ResourceException(
                        "Geometry shaders are not supported on current hardware");
            }

            int geometryVersion = extractShaderVersion(geometryCode);
            if (geometryVersion != vertexVersion) {
                throw new ResourceException(
                        "GLSL versions inconsistent between vertex (" + vertexVersion +
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
            if (output.equals("gl_FragColor")) {
                throw new ResourceException(
                        "Cannot specify color buffer for gl_FragColor, it is implicit");
            } else if (output.startsWith("gl_FragData[")) {
                throw new ResourceException(
                        "Cannot specify color buffer for gl_FragData[n], it is implicit");
            }

            Integer buffer = mappedBuffers.get(output);
            if (buffer >= framework.getCapabilities().getMaxColorBuffers()) {
                throw new ResourceException(
                        "Current hardware cannot support a color buffer = " + buffer);
            }
            if (buffer >= 0 && assignedBuffers.contains(buffer)) {
                throw new ResourceException(
                        "Multiple output variables mapped to the same color buffer");
            }
            assignedBuffers.add(buffer);
        }

        detectedShaderVersion = vertexVersion;
    }

    @Override
    protected ShaderImpl.ShaderHandle allocate(OpenGLContext ctx) {
        int geometryID = (geometryCode == null ? 0 : createNewGeometryShader(ctx));
        return new ShaderImpl.ShaderHandle(framework, createNewProgram(ctx),
                                           createNewVertexShader(ctx),
                                           createNewFragmentShader(ctx), geometryID);
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, ShaderImpl.ShaderHandle handle) {
        compileShader(ctx, handle.vertexShaderID, cleanSourceCode(vertexCode));
        compileShader(ctx, handle.fragmentShaderID, cleanSourceCode(fragmentCode));
        if (geometryCode != null) {
            compileShader(ctx, handle.geometryShaderID, cleanSourceCode(geometryCode));
        }

        attachShader(ctx, handle.programID, handle.vertexShaderID);
        attachShader(ctx, handle.programID, handle.fragmentShaderID);
        if (geometryCode != null) {
            attachShader(ctx, handle.programID, handle.geometryShaderID);
        }

        // bind explicit varying outs before we link
        for (Map.Entry<String, Integer> binding : mappedBuffers.entrySet()) {
            if (binding.getValue() >= 0) {
                bindFragmentLocation(ctx, handle.programID, binding.getKey(),
                                     binding.getValue());
            }
        }

        linkProgram(ctx, handle.programID);

        // query linked program state
        detectedUniforms = getUniforms(ctx, handle.programID);
        detectedAttributes = getAttributes(ctx, handle.programID);

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
        return new ShaderImpl(handle, detectedShaderVersion, detectedUniforms,
                              detectedAttributes, detectedBufferMapping);
    }

    protected abstract int createNewProgram(OpenGLContext context);

    protected abstract int createNewVertexShader(OpenGLContext context);

    protected abstract int createNewFragmentShader(OpenGLContext context);

    protected abstract int createNewGeometryShader(OpenGLContext context);

    protected abstract void compileShader(OpenGLContext context, int shaderID,
                                          String code);

    protected abstract void attachShader(OpenGLContext context, int programID,
                                         int shaderID);

    protected abstract void linkProgram(OpenGLContext context, int programID);

    protected abstract List<Shader.Uniform> getUniforms(OpenGLContext context,
                                                        int programID);

    protected abstract List<Shader.Attribute> getAttributes(OpenGLContext context,
                                                            int programID);

    protected abstract void bindFragmentLocation(OpenGLContext context, int programID,
                                                 String variable, int buffer);

    protected abstract int getFragmentLocation(OpenGLContext context, int programID,
                                               String variable);

    private int extractShaderVersion(String code) {
        Matcher m = VERSION.matcher(code);
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
}

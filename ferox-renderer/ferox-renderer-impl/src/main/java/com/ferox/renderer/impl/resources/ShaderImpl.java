package com.ferox.renderer.impl.resources;

import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ShaderImpl extends AbstractResource<ShaderImpl.ShaderHandle>
        implements Shader {
    private final int glslVersion;
    private final List<Uniform> uniforms;
    private final List<Attribute> attributes;
    private final Map<String, Integer> bufferMap;

    public ShaderImpl(ShaderHandle handle, int glslVersion, List<Uniform> uniforms,
                      List<Attribute> attributes, Map<String, Integer> bufferMap) {
        super(handle);
        this.glslVersion = glslVersion;

        this.uniforms = Collections.unmodifiableList(uniforms);
        this.attributes = Collections.unmodifiableList(attributes);
        this.bufferMap = Collections.unmodifiableMap(bufferMap);
    }

    @Override
    public List<Uniform> getUniforms() {
        return uniforms;
    }

    @Override
    public Uniform getUniform(String name) {
        for (Uniform u : uniforms) {
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public Attribute getAttribute(String name) {
        for (Attribute a : attributes) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public int getColorBuffer(String outVariableName) {
        // FIXME is it more efficient to store the reserved buffer mappings into the map
        // if that is what is being used by the shader?
        // if they are put in the map, is the builder that is responsible for this?
        // it might happen naturally with OpenGL's APIs anyways
        Integer mappedBuffer = bufferMap.get(outVariableName);
        if (mappedBuffer != null) {
            return mappedBuffer;
        } else if (outVariableName.startsWith("gl_FragData[")) {
            return Integer.parseInt(
                    outVariableName.substring(12, outVariableName.length() - 1));
        } else {
            // assume it's gl_FragColor or fallback to default 0th buffer
            return 0;
        }
    }

    @Override
    public int getGLSLVersion() {
        return glslVersion;
    }

    @Override
    public boolean hasGeometryShader() {
        return getHandle().geometryShaderID > 0;
    }

    public static class UniformImpl implements Uniform {
        private final VariableType type;
        private final String name;
        private final int index;
        private final int length;

        public UniformImpl(VariableType type, int length, String name, int index) {
            this.type = type;
            this.length = length;
            this.name = name;
            this.index = index;
        }

        @Override
        public VariableType getType() {
            return type;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isReserved() {
            return name.startsWith("gl_");
        }

        @Override
        public boolean isArray() {
            return length > 1;
        }
    }

    public static class AttributeImpl implements Attribute {
        private final VariableType type;
        private final String name;
        private final int index;

        public AttributeImpl(VariableType type, String name, int index) {
            this.type = type;
            this.name = name;
            this.index = index;
        }

        @Override
        public VariableType getType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isReserved() {
            return name.startsWith("gl_");
        }
    }

    public static class ShaderHandle extends ResourceHandle {
        public final int programID;
        public final int vertexShaderID;
        public final int fragmentShaderID;
        public final int geometryShaderID;

        public ShaderHandle(FrameworkImpl framework, int programID, int vertexShaderID,
                            int fragmentShaderID, int geometryShaderID) {
            super(framework);
            this.programID = programID;
            this.vertexShaderID = vertexShaderID;
            this.fragmentShaderID = fragmentShaderID;
            this.geometryShaderID = geometryShaderID;
        }

        @Override
        protected void destroyImpl(OpenGLContext context) {
            getFramework().getResourceFactory().deleteShader(context, this);
        }
    }
}

package com.ferox.renderer.impl.resources;

import com.ferox.renderer.DataType;
import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ShaderImpl extends AbstractResource<ShaderImpl.ShaderHandle> implements Shader {
    private final int glslVersion;
    private final List<UniformImpl> uniforms;
    private final List<AttributeImpl> attributes;
    private final Map<String, Integer> bufferMap;

    public ShaderImpl(ShaderHandle handle, int glslVersion, List<UniformImpl> uniforms,
                      List<AttributeImpl> attributes, Map<String, Integer> bufferMap) {
        super(handle);
        this.glslVersion = glslVersion;

        this.uniforms = Collections.unmodifiableList(uniforms);
        this.attributes = Collections.unmodifiableList(attributes);
        this.bufferMap = Collections.unmodifiableMap(bufferMap);
    }

    @Override
    public List<? extends Uniform> getUniforms() {
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
    public List<? extends Attribute> getAttributes() {
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
        Integer mappedBuffer = bufferMap.get(outVariableName);
        if (mappedBuffer != null) {
            return mappedBuffer;
        } else {
            // -1 is flag for unknown output variable
            return -1;
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

        public final FloatBuffer floatValues; // non-null for float types only
        public final IntBuffer intValues; // non-null for int and sampler types only

        // for samplers, the intValues holds a unique pre-assigned texture unit
        // reserved for any sampler assigned to the uniform
        public TextureImpl.TextureHandle texture;

        public boolean initialized;

        public UniformImpl(VariableType type, int length, String name, int index) {
            this.type = type;
            this.length = length;
            this.name = name;
            this.index = index;

            int bufferSize = type.getPrimitiveCount() * length;
            if (type == VariableType.FLOAT || type == VariableType.VEC2 ||
                type == VariableType.VEC3 || type == VariableType.VEC4 ||
                type == VariableType.MAT2 || type == VariableType.MAT3 ||
                type == VariableType.MAT4) {
                // only floating point types
                floatValues = BufferUtil.newByteBuffer(DataType.FLOAT, bufferSize).asFloatBuffer();
                intValues = null;
            } else {
                // all other types are stored as ints
                floatValues = null;
                intValues = BufferUtil.newByteBuffer(DataType.INT, bufferSize).asIntBuffer();
            }

            texture = null;
            initialized = false;
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

        public ShaderHandle(FrameworkImpl framework, int programID, int vertexShaderID, int fragmentShaderID,
                            int geometryShaderID) {
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
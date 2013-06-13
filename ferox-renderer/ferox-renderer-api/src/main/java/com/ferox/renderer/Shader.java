package com.ferox.renderer;

import java.util.Map;

/**
 *
 */
public interface Shader {
    // FIXME will it be worth it to support indexable attributes and uniforms instead
    // of requiring a map lookup everytime? Also, I should merge attribute and uniform
    // types and just specify that certain types aren't supported in every spot
    public enum Attribute {
        FLOAT(1, 1),
        FLOAT_VEC2(2, 1),
        FLOAT_VEC3(3, 1),
        FLOAT_VEC4(4, 1),
        FLOAT_MAT2(2, 2),
        FLOAT_MAT3(3, 3),
        FLOAT_MAT4(4, 4),
        UNSUPPORTED(0, 0);

        private final int row;
        private final int col;

        Attribute(int r, int c) {
            row = r;
            col = c;
        }

        public int getRowCount() {
            return row;
        }

        public int getColumnCount() {
            return col;
        }
    }

    public class Uniform {
        public static enum UniformType {
            FLOAT(1),
            FLOAT_VEC2(2),
            FLOAT_VEC3(3),
            FLOAT_VEC4(4),
            FLOAT_MAT2(4),
            FLOAT_MAT3(9),
            FLOAT_MAT4(16),

            INT(1),
            INT_VEC2(2),
            INT_VEC3(3),
            INT_VEC4(4),

            TEXTURE_1D(1),
            TEXTURE_2D(1),
            TEXTURE_3D(1),
            TEXTURE_CUBEMAP(1),
            SHADOW_MAP(1),

            BOOL(1),
            UNSUPPORTED(0);

            private final int primCount;

            private UniformType(int primCount) {
                this.primCount = primCount;
            }

            public int getPrimitiveCount() {
                return primCount;
            }
        }

        private final UniformType type;
        private final int length;
        private final String name;

        public Uniform(String name, UniformType type, int length) {
            if (name == null) {
                throw new NullPointerException("Cannot specify a null name");
            }
            if (type == null) {
                throw new NullPointerException("Cannot specify a null uniform type");
            }

            if (length < 1) {
                throw new IllegalArgumentException(
                        "Cannot specify length < 1: " + length);
            }

            this.name = name;
            this.type = type;
            this.length = length;
        }

        /**
         * Get the value type of this uniform. If getLength() > 1, then this is the
         * component type of the array for the uniform.
         *
         * @return The type of this uniform
         */
        public UniformType getType() {
            return type;
        }

        /**
         * Return the number of primitives of getType() used by this uniform. If it's > 1,
         * then the uniform represents an array.
         *
         * @return Size of the uniform, in units of getType()
         */
        public int getLength() {
            return length;
        }

        /**
         * Return the name of the uniform as declared in the glsl code of the uniform's
         * owner.
         *
         * @return The name of the uniform
         */
        public String getName() {
            return name;
        }

        public boolean isReserved() {
            return name.startsWith("gl_");
        }

        public boolean isArray() {
            return length > 1;
        }
    }

    public Map<String, Uniform> getUniforms();

    public Uniform getUniform(String name);

    public Map<String, Attribute> getAttributes();

    public Attribute getAttribute(String name);

    public int getGLSLVersion();

    public boolean hasVertexShader();

    public boolean hasGeometryShader();

    public boolean hasFragmentShader();
}

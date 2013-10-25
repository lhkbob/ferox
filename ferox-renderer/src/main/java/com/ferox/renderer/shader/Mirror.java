package com.ferox.renderer.shader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
public interface Mirror {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface TypeName {
        public String value();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface BuiltIn {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface StructParameter {
        public String name();

        public Class<? extends Mirror> type();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Struct {
        public StructParameter[] value();
    }

    @BuiltIn
    @TypeName("float")
    public static interface Float extends Mirror {
    }

    @BuiltIn
    @TypeName("int")
    public static interface Int extends Mirror {
    }

    @BuiltIn
    @TypeName("uint")
    public static interface UInt extends Mirror {
    }

    @BuiltIn
    @TypeName("bool")
    public static interface Bool extends Mirror {
    }

    @BuiltIn
    @TypeName("mat2")
    public static interface Mat2 extends Mirror {
    }

    @BuiltIn
    @TypeName("mat3")
    public static interface Mat3 extends Mirror {
    }

    @BuiltIn
    @TypeName("mat4")
    public static interface Mat4 extends Mirror {
    }

    @BuiltIn
    @TypeName("vec2")
    public static interface Vec2 extends Mirror {
    }

    @BuiltIn
    @TypeName("vec3")
    public static interface Vec3 extends Mirror {
    }

    @BuiltIn
    @TypeName("vec4")
    public static interface Vec4 extends Mirror {
    }

    @BuiltIn
    @TypeName("ivec2")
    public static interface IVec2 extends Mirror {
    }

    @BuiltIn
    @TypeName("ivec3")
    public static interface IVec3 extends Mirror {
    }

    @BuiltIn
    @TypeName("ovec4")
    public static interface IVec4 extends Mirror {
    }

    @BuiltIn
    @TypeName("uvec2")
    public static interface UVec2 extends Mirror {
    }

    @BuiltIn
    @TypeName("uvec3")
    public static interface UVec3 extends Mirror {
    }

    @BuiltIn
    @TypeName("uvec4")
    public static interface UVec4 extends Mirror {
    }

    @BuiltIn
    @TypeName("bvec2")
    public static interface BVec2 extends Mirror {
    }

    @BuiltIn
    @TypeName("bvec3")
    public static interface BVec3 extends Mirror {
    }

    @BuiltIn
    @TypeName("bvec4")
    public static interface BVec4 extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler1D")
    public static interface Sampler1D extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler2D")
    public static interface Sampler2D extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler3D")
    public static interface Sampler3D extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler1DArray")
    public static interface Sampler1DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler2DArray")
    public static interface Sampler2DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("samplerCube")
    public static interface SamplerCube extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler1DShadow")
    public static interface Sampler1DShadow extends Mirror {
    }

    @BuiltIn
    @TypeName("sampler2DShadow")
    public static interface Sampler2DShadow extends Mirror {
    }

    @BuiltIn
    @TypeName("isampler1D")
    public static interface ISampler1D extends Mirror {
    }

    @BuiltIn
    @TypeName("isampler2D")
    public static interface ISampler2D extends Mirror {
    }

    @BuiltIn
    @TypeName("isampler3D")
    public static interface ISampler3D extends Mirror {
    }

    @BuiltIn
    @TypeName("isampler1DArray")
    public static interface ISampler1DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("isampler2DArray")
    public static interface ISampler2DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("isamplerCube")
    public static interface ISamplerCube extends Mirror {
    }

    @BuiltIn
    @TypeName("usampler1D")
    public static interface USampler1D extends Mirror {
    }

    @BuiltIn
    @TypeName("usampler2D")
    public static interface USampler2D extends Mirror {
    }

    @BuiltIn
    @TypeName("usampler3D")
    public static interface USampler3D extends Mirror {
    }

    @BuiltIn
    @TypeName("usampler1DArray")
    public static interface USampler1DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("usampler2DArray")
    public static interface USampler2DArray extends Mirror {
    }

    @BuiltIn
    @TypeName("usamplerCube")
    public static interface USamplerCube extends Mirror {
    }
}

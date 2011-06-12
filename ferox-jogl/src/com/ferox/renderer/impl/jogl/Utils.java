package com.ferox.renderer.impl.jogl;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOp;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.EnvMode;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.Renderer.StencilOp;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.resource.TextureFormat;

/**
 * Utils provides conversions for the commonly used enums in Resources and Renderers
 * to their appropriate GL enum values, as well as some other useful utilities.
 * 
 * @author Michael Ludwig
 */
public class Utils {
    /**
     * Return the UniformType enum value associated with the returned GL2 enum
     * for uniform variable type. Returns null if there's no matching
     * UniformType.
     */
    public static UniformType getUniformType(int type) {
        switch (type) {
        case GL2.GL_FLOAT:
            return UniformType.FLOAT;
        case GL2.GL_FLOAT_VEC2:
            return UniformType.FLOAT_VEC2;
        case GL2.GL_FLOAT_VEC3:
            return UniformType.FLOAT_VEC3;
        case GL2.GL_FLOAT_VEC4:
            return UniformType.FLOAT_VEC4;

        case GL2.GL_FLOAT_MAT2:
            return UniformType.FLOAT_MAT2;
        case GL2.GL_FLOAT_MAT3:
            return UniformType.FLOAT_MAT3;
        case GL2.GL_FLOAT_MAT4:
            return UniformType.FLOAT_MAT4;

        case GL2.GL_INT:
            return UniformType.INT;
        case GL2.GL_INT_VEC2:
            return UniformType.INT_VEC2;
        case GL2.GL_INT_VEC3:
            return UniformType.INT_VEC3;
        case GL2.GL_INT_VEC4:
            return UniformType.INT_VEC4;

        case GL2.GL_BOOL:
            return UniformType.BOOL;

        case GL2.GL_SAMPLER_1D:
            return UniformType.TEXTURE_1D;
        case GL2.GL_SAMPLER_2D:
            return UniformType.TEXTURE_2D;
        case GL2.GL_SAMPLER_3D:
            return UniformType.TEXTURE_3D;
        case GL2.GL_SAMPLER_CUBE:
            return UniformType.TEXTURE_CUBEMAP;
        case GL2.GL_SAMPLER_2D_SHADOW:
            return UniformType.SHADOW_MAP;
        }

        return null;
    }

    /**
     * Return the AttributeType enum value associated with the returned GL2 enum
     * for attribute variable type. Returns null if there's no matching
     * AttributeType.
     */
    public static AttributeType getAttributeType(int type) {
        switch (type) {
        case GL2.GL_FLOAT:
            return AttributeType.FLOAT;
        case GL2.GL_FLOAT_VEC2:
            return AttributeType.FLOAT_VEC2;
        case GL2.GL_FLOAT_VEC3:
            return AttributeType.FLOAT_VEC3;
        case GL2.GL_FLOAT_VEC4:
            return AttributeType.FLOAT_VEC4;
        case GL2.GL_FLOAT_MAT2:
            return AttributeType.FLOAT_MAT2;
        case GL2.GL_FLOAT_MAT3:
            return AttributeType.FLOAT_MAT3;
        case GL2.GL_FLOAT_MAT4:
            return AttributeType.FLOAT_MAT4;
        }

        return null;
    }
    
    /**
     * Return the GL shader type enum for the given type.
     */
    public static int getGLShaderType(ShaderType type) {
        switch(type) {
        case FRAGMENT:
            return GL2GL3.GL_FRAGMENT_SHADER;
        case GEOMETRY:
            return GL2GL3.GL_GEOMETRY_SHADER;
        case VERTEX:
            return GL2GL3.GL_VERTEX_SHADER;
        default:
            return -1;
        }
    }

    /** EffectType can't be null. */
    public static int getGLPolygonConnectivity(PolygonType type) {
        switch (type) {
        case LINES:
            return GL2.GL_LINES;
        case POINTS:
            return GL2.GL_POINTS;
        case QUAD_STRIP:
            return GL2.GL_QUAD_STRIP;
        case QUADS:
            return GL2.GL_QUADS;
        case TRIANGLE_STRIP:
            return GL2.GL_TRIANGLE_STRIP;
        case TRIANGLES:
            return GL2.GL_TRIANGLES;
        }

        return -1;
    }

    /** Test must not be null. */
    public static int getGLPixelTest(Comparison test) {
        switch (test) {
        case ALWAYS:
            return GL2.GL_ALWAYS;
        case EQUAL:
            return GL2.GL_EQUAL;
        case GEQUAL:
            return GL2.GL_GEQUAL;
        case GREATER:
            return GL2.GL_GREATER;
        case LEQUAL:
            return GL2.GL_LEQUAL;
        case LESS:
            return GL2.GL_LESS;
        case NEVER:
            return GL2.GL_NEVER;
        case NOT_EQUAL:
            return GL2.GL_NOTEQUAL;
        }

        return -1;
    }

    /**
     * Return the gl enum associated with the given filter for minification.
     * filter must not be null.
     */
    public static int getGLMinFilter(Filter filter) {
        switch (filter) {
        case LINEAR:
            return GL2.GL_LINEAR;
        case NEAREST:
            return GL2.GL_NEAREST;
        case MIPMAP_LINEAR:
            return GL2.GL_LINEAR_MIPMAP_LINEAR;
        case MIPMAP_NEAREST:
            return GL2.GL_NEAREST_MIPMAP_NEAREST;
        }

        return -1;
    }

    /**
     * Return the gl enum associated with the given filter for magnification.
     * filter must not be null.
     */
    public static int getGLMagFilter(Filter filter) {
        switch (filter) {
        case LINEAR:
        case MIPMAP_LINEAR:
            return GL2.GL_LINEAR;
        case NEAREST:
        case MIPMAP_NEAREST:
            return GL2.GL_NEAREST;
        }

        return -1;
    }

    /** Wrap must not be null. */
    public static int getGLWrapMode(WrapMode wrap) {
        switch (wrap) {
        case CLAMP:
            return GL2.GL_CLAMP_TO_EDGE;
        case MIRROR:
            return GL2.GL_MIRRORED_REPEAT;
        case REPEAT:
            return GL2.GL_REPEAT;
        }

        return -1;
    }

    /** Face must be one of the constants in TextureCubeMap (0 - 5). */
    public static int getGLCubeFace(int face) {
        switch (face) {
        case Texture.PX:
            return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
        case Texture.NX:
            return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
        case Texture.PY:
            return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
        case Texture.NY:
            return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
        case Texture.PZ:
            return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
        case Texture.NZ:
            return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }

        return -1;
    }

    /** Target must not be null. */
    public static int getGLTextureTarget(Target tar) {
        switch (tar) {
        case T_1D:
            return GL2.GL_TEXTURE_1D;
        case T_2D:
            return GL2.GL_TEXTURE_2D;
        case T_3D:
            return GL2.GL_TEXTURE_3D;
        case T_CUBEMAP:
            return GL2.GL_TEXTURE_CUBE_MAP;
        }

        return -1;
    }

    /**
     * Format must not be null. Returns an enum for the src format in
     * glTexImage. Returns -1 for compressed formats.
     */
    public static int getGLSrcFormat(TextureFormat format) {
        switch (format) {
        // packed of the RGBA variety (packed type distinguishes them)
        case ABGR_1555:
        case ABGR_4444:
        case ABGR_8888:
            return GL2.GL_RGBA;
        case RGBA_4444:
        case RGBA_5551:
        case RGBA_8888:
            return GL2.GL_RGBA;

            // packed of the BGRA variety (packed type distinguishes them)
        case ARGB_1555:
        case ARGB_4444:
        case ARGB_8888:
            return GL2.GL_BGRA;
        case BGRA_4444:
        case BGRA_5551:
        case BGRA_8888:
            return GL2.GL_BGRA;

            // packed and unpacked RGB types
        case RGB:
        case RGB_565:
        case RGB_FLOAT:
            return GL2.GL_RGB;

            // packed and unpacked BGR types
        case BGR:
            return GL2.GL_BGR;
        case BGR_565:
            return GL2.GL_RGB; // type swaps the ordering

            // unpacked RGBA and BGRA types
        case RGBA:
        case RGBA_FLOAT:
            return GL2.GL_RGBA;
        case BGRA:
            return GL2.GL_BGRA;

            // depth formats
        case DEPTH:
            return GL2.GL_DEPTH_COMPONENT;

            // alpha formats
        case ALPHA:
        case ALPHA_FLOAT:
            return GL2.GL_ALPHA;

            // luminance formats
        case LUMINANCE:
        case LUMINANCE_FLOAT:
            return GL2.GL_LUMINANCE;

            // LA formats
        case LUMINANCE_ALPHA:
        case LUMINANCE_ALPHA_FLOAT:
            return GL2.GL_LUMINANCE_ALPHA;

        }

        // a compressed type
        return -1;
    }

    /**
     * Format and type can't be null. Returns an enum for the dst format in
     * glTexImage.
     */
    public static int getGLDstFormat(TextureFormat format, DataType type) {
        switch (format) {
        // packed RGB5_A1
        case ABGR_1555:
        case ARGB_1555:
        case RGBA_5551:
        case BGRA_5551:
            return GL2.GL_RGB5_A1;

            // packed RGBA4
        case ABGR_4444:
        case ARGB_4444:
        case RGBA_4444:
        case BGRA_4444:
            return GL2.GL_RGBA4;

            // packed RGBA8
        case ABGR_8888:
        case ARGB_8888:
        case RGBA_8888:
        case BGRA_8888:
            return GL2.GL_RGBA8;

            // packed RGB8
        case RGB_565:
        case BGR_565:
            return GL2.GL_RGB5;

            // unclamped floating point
        case RGB_FLOAT:
            return GL2.GL_RGB32F;
        case RGBA_FLOAT:
            return GL2.GL_RGBA32F;
        case ALPHA_FLOAT:
            return GL2.GL_ALPHA32F;
        case LUMINANCE_FLOAT:
            return GL2.GL_LUMINANCE32F;
        case LUMINANCE_ALPHA_FLOAT:
            return GL2.GL_LUMINANCE_ALPHA32F;

            // DXT_n compression
        case RGB_DXT1:
            return GL2.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case RGBA_DXT1:
            return GL2.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        case RGBA_DXT3:
            return GL2.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
        case RGBA_DXT5:
            return GL2.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;

            // if we've gotten here, we have a type-less format, and have to
            // take the type into account
        case ALPHA:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_ALPHA8;
            else if (type == DataType.UNSIGNED_SHORT || type == DataType.UNSIGNED_INT)
                return GL2.GL_ALPHA16;
            else
                return GL2.GL_ALPHA;
        case LUMINANCE:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_LUMINANCE8;
            else if (type == DataType.UNSIGNED_SHORT || type == DataType.UNSIGNED_INT)
                return GL2.GL_LUMINANCE16;
            else
                return GL2.GL_LUMINANCE;
        case LUMINANCE_ALPHA:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_LUMINANCE8_ALPHA8;
            else if (type == DataType.UNSIGNED_SHORT || type == DataType.UNSIGNED_INT)
                return GL2.GL_LUMINANCE16_ALPHA16;
            else
                return GL2.GL_LUMINANCE_ALPHA;
        case DEPTH:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_DEPTH_COMPONENT16;
            else if (type == DataType.UNSIGNED_SHORT)
                return GL2.GL_DEPTH_COMPONENT24;
            else if (type == DataType.UNSIGNED_INT)
                return GL2.GL_DEPTH_COMPONENT32;
            else
                return GL2.GL_DEPTH_COMPONENT;
        case RGB:
        case BGR:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_RGB8;
            else if (type == DataType.UNSIGNED_SHORT || type == DataType.UNSIGNED_INT)
                return GL2.GL_RGB16;
            else
                return GL2.GL_RGB;
        case RGBA:
        case BGRA:
            if (type == DataType.UNSIGNED_BYTE)
                return GL2.GL_RGBA8;
            else if (type == DataType.UNSIGNED_SHORT || type == DataType.UNSIGNED_INT)
                return GL2.GL_RGBA16;
            else
                return GL2.GL_RGBA;
        }

        return -1;
    }

    /**
     * Format must not be null. Returns an appropriate data type for packed
     * source format. Returns -1 if it's not a packed type. These are chosen
     * with the assumption of big-endian byte ordering.
     */
    public static int getGLPackedType(TextureFormat format) {
        switch (format) {
        // packed ABGR and ARGB types
        case ABGR_1555:
        case ARGB_1555:
            return GL2.GL_UNSIGNED_SHORT_1_5_5_5_REV;
        case ABGR_4444:
        case ARGB_4444:
            return GL2.GL_UNSIGNED_SHORT_4_4_4_4_REV;
        case ABGR_8888:
        case ARGB_8888:
            return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;

            // packed BGRA and RGBA types
        case BGRA_5551:
        case RGBA_5551:
            return GL2.GL_UNSIGNED_SHORT_5_5_5_1;
        case BGRA_4444:
        case RGBA_4444:
            return GL2.GL_UNSIGNED_SHORT_4_4_4_4;
        case BGRA_8888:
        case RGBA_8888:
            return GL2.GL_UNSIGNED_INT_8_8_8_8;

            // packed BGR and RGB types
        case BGR_565:
            return GL2.GL_UNSIGNED_SHORT_5_6_5_REV;
        case RGB_565:
            return GL2.GL_UNSIGNED_SHORT_5_6_5;
        }

        // not a packed type
        return -1;
    }

    /**
     * This shouldn't be used for packed data
     * types.
     */
    public static int getGLType(DataType type) {
        switch(type) {
        case FLOAT: return GL2.GL_FLOAT;
        case UNSIGNED_BYTE: return GL2.GL_UNSIGNED_BYTE;
        case UNSIGNED_INT: return GL2.GL_UNSIGNED_INT;
        case UNSIGNED_SHORT: return GL2.GL_UNSIGNED_SHORT;
        }

        return -1;
    }

    /** Func must not be null. */
    public static int getGLBlendEquation(BlendFunction func) {
        switch (func) {
        case ADD:
            return GL2.GL_FUNC_ADD;
        case MAX:
            return GL2.GL_MAX;
        case MIN:
            return GL2.GL_MIN;
        case REVERSE_SUBTRACT:
            return GL2.GL_FUNC_REVERSE_SUBTRACT;
        case SUBTRACT:
            return GL2.GL_FUNC_SUBTRACT;
        }

        return -1;
    }

    /** Src must not be null. */
    public static int getGLBlendFactor(BlendFactor src) {
        switch (src) {
        case ZERO:
            return GL2.GL_ZERO;
        case ONE:
            return GL2.GL_ONE;
        case SRC_COLOR:
            return GL2.GL_SRC_COLOR;
        case ONE_MINUS_SRC_COLOR:
            return GL2.GL_ONE_MINUS_SRC_COLOR;
        case SRC_ALPHA:
            return GL2.GL_SRC_ALPHA;
        case ONE_MINUS_SRC_ALPHA:
            return GL2.GL_ONE_MINUS_SRC_ALPHA;
        case SRC_ALPHA_SATURATE:
            return GL2.GL_SRC_ALPHA_SATURATE;
        }

        return -1;
    }

    /**
     * Should not be called with NONE or null, as no gl enum exists to match it.
     */
    public static int getGLPolygonMode(DrawStyle style) {
        switch (style) {
        case LINE:
            return GL2.GL_LINE;
        case POINT:
            return GL2.GL_POINT;
        case SOLID:
            return GL2.GL_FILL;
        }

        return -1;
    }

    /** Op must not be null. */
    public static int getGLStencilOp(StencilOp op, boolean wrapSupported) {
        switch (op) {
        case DECREMENT:
            return GL2.GL_DECR;
        case DECREMENT_WRAP:
            return (wrapSupported ? GL2.GL_DECR_WRAP : GL2.GL_DECR);
        case INCREMENT:
            return GL2.GL_INCR;
        case INCREMENT_WRAP:
            return (wrapSupported ? GL2.GL_INCR_WRAP : GL2.GL_INCR);
        case ZERO:
            return GL2.GL_ZERO;
        case KEEP:
            return GL2.GL_KEEP;
        case REPLACE:
            return GL2.GL_REPLACE;
        case INVERT:
            return GL2.GL_INVERT;
        }

        return -1;
    }

    /** Mode must not be null. */
    public static int getGLTexEnvMode(EnvMode mode) {
        switch (mode) {
        case REPLACE:
            return GL2.GL_REPLACE;
        case DECAL:
            return GL2.GL_DECAL;
        case MODULATE:
            return GL2.GL_MODULATE;
        case BLEND:
            return GL2.GL_BLEND;
        case COMBINE:
            return GL2.GL_COMBINE;
        }

        return -1;
    }

    /**
     * Should not be called with ATTRIBUTE or null, as nothing parallels its meaning.
     */
    public static int getGLTexGen(TexCoordSource gen) {
        switch (gen) {
        case EYE:
            return GL2.GL_EYE_LINEAR;
        case OBJECT:
            return GL2.GL_OBJECT_LINEAR;
        case SPHERE:
            return GL2.GL_SPHERE_MAP;
        case REFLECTION:
            return GL2.GL_REFLECTION_MAP;
        case NORMAL:
            return GL2.GL_NORMAL_MAP;
        }

        return -1;
    }
    
    /** Coord must be null */
    public static int getGLTexCoord(TexCoord coord, boolean forEnable) {
        switch(coord) {
        case Q:
            return (forEnable ? GL2.GL_TEXTURE_GEN_Q : GL2.GL_Q);
        case R:
            return (forEnable ? GL2.GL_TEXTURE_GEN_R : GL2.GL_R);
        case S:
            return (forEnable ? GL2.GL_TEXTURE_GEN_S : GL2.GL_S);
        case T:
            return (forEnable ? GL2.GL_TEXTURE_GEN_T : GL2.GL_T);
        }
        
        return -1;
    }

    /** Func must not be null. */
    public static int getGLCombineFunc(CombineFunction func) {
        switch (func) {
        case ADD:
            return GL2.GL_ADD;
        case ADD_SIGNED:
            return GL2.GL_ADD_SIGNED;
        case DOT3_RGB:
            return GL2.GL_DOT3_RGB;
        case DOT3_RGBA:
            return GL2.GL_DOT3_RGBA;
        case INTERPOLATE:
            return GL2.GL_INTERPOLATE;
        case MODULATE:
            return GL2.GL_MODULATE;
        case REPLACE:
            return GL2.GL_REPLACE;
        case SUBTRACT:
            return GL2.GL_SUBTRACT;
        }

        return -1;
    }

    /** Op must not be null. */
    public static int getGLCombineOp(CombineOp op) {
        switch (op) {
        case ALPHA:
            return GL2.GL_SRC_ALPHA;
        case COLOR:
            return GL2.GL_SRC_COLOR;
        case ONE_MINUS_ALPHA:
            return GL2.GL_ONE_MINUS_SRC_ALPHA;
        case ONE_MINUS_COLOR:
            return GL2.GL_ONE_MINUS_SRC_COLOR;
        }

        return -1;
    }

    /** Src must not be null. */
    public static int getGLCombineSrc(CombineSource src) {
        switch (src) {
        case CONST_COLOR:
            return GL2.GL_CONSTANT;
        case CURR_TEX:
            return GL2.GL_TEXTURE;
        case PREV_TEX:
            return GL2.GL_PREVIOUS;
        case VERTEX_COLOR:
            return GL2.GL_PRIMARY_COLOR;
        case TEX0:
            return GL2.GL_TEXTURE0;
        case TEX1:
            return GL2.GL_TEXTURE1;
        case TEX2:
            return GL2.GL_TEXTURE2;
        case TEX3:
            return GL2.GL_TEXTURE3;
        case TEX4:
            return GL2.GL_TEXTURE4;
        case TEX5:
            return GL2.GL_TEXTURE5;
        case TEX6:
            return GL2.GL_TEXTURE6;
        case TEX7:
            return GL2.GL_TEXTURE7;
        case TEX8:
            return GL2.GL_TEXTURE8;
        case TEX9:
            return GL2.GL_TEXTURE9;
        case TEX10:
            return GL2.GL_TEXTURE10;
        case TEX11:
            return GL2.GL_TEXTURE11;
        case TEX12:
            return GL2.GL_TEXTURE12;
        case TEX13:
            return GL2.GL_TEXTURE13;
        case TEX14:
            return GL2.GL_TEXTURE14;
        case TEX15:
            return GL2.GL_TEXTURE15;
        case TEX16:
            return GL2.GL_TEXTURE16;
        case TEX17:
            return GL2.GL_TEXTURE17;
        case TEX18:
            return GL2.GL_TEXTURE18;
        case TEX19:
            return GL2.GL_TEXTURE19;
        case TEX20:
            return GL2.GL_TEXTURE20;
        case TEX21:
            return GL2.GL_TEXTURE21;
        case TEX22:
            return GL2.GL_TEXTURE22;
        case TEX23:
            return GL2.GL_TEXTURE23;
        case TEX24:
            return GL2.GL_TEXTURE24;
        case TEX25:
            return GL2.GL_TEXTURE25;
        case TEX26:
            return GL2.GL_TEXTURE26;
        case TEX27:
            return GL2.GL_TEXTURE27;
        case TEX28:
            return GL2.GL_TEXTURE28;
        case TEX29:
            return GL2.GL_TEXTURE29;
        case TEX30:
            return GL2.GL_TEXTURE30;
        case TEX31:
            return GL2.GL_TEXTURE31;
        }

        return -1;
    }

    /**
     * Utility method to invoke a Runnable on the AWT event dispatch thread
     * (e.g. for modifying AWT and Swing components). This will throw a runtime
     * exception if a problem occurs. It works properly if called from the AWT
     * thread. This should be used when EventQueue.invokeAndWait() or
     * SwingUtilities.invokeAndWait() would be used, except that this is thread
     * safe.
     */
    public static void invokeOnAWTThread(Runnable r, boolean block) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            if (block) {
                try {
                    System.out.println("queueing awt task");
                    EventQueue.invokeAndWait(r);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                EventQueue.invokeLater(r);
            }
        }
    }
}

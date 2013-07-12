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

import com.ferox.renderer.FixedFunctionRenderer.*;
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.impl.ContextManager;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.Uniform.UniformType;
import com.ferox.renderer.texture.Texture;
import com.ferox.renderer.texture.Texture.Filter;
import com.ferox.renderer.texture.Texture.Target;
import com.ferox.renderer.texture.Texture.WrapMode;
import com.ferox.renderer.texture.TextureFormat;

import javax.media.opengl.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utils provides conversions for the commonly used enums in Resources and Renderers to
 * their appropriate GL enum values, as well as some other useful utilities.
 *
 * @author Michael Ludwig
 */
public class Utils {
    /**
     * Return the UniformType enum value associated with the returned GL2 enum for uniform
     * variable type. Returns null if there's no matching UniformType.
     */
    public static UniformType getUniformType(int type) {
        switch (type) {
        case GL.GL_FLOAT:
            return UniformType.FLOAT;
        case GL2ES2.GL_FLOAT_VEC2:
            return UniformType.FLOAT_VEC2;
        case GL2ES2.GL_FLOAT_VEC3:
            return UniformType.FLOAT_VEC3;
        case GL2ES2.GL_FLOAT_VEC4:
            return UniformType.FLOAT_VEC4;

        case GL2ES2.GL_FLOAT_MAT2:
            return UniformType.FLOAT_MAT2;
        case GL2ES2.GL_FLOAT_MAT3:
            return UniformType.FLOAT_MAT3;
        case GL2ES2.GL_FLOAT_MAT4:
            return UniformType.FLOAT_MAT4;

        case GL2ES2.GL_INT:
            return UniformType.INT;
        case GL2ES2.GL_INT_VEC2:
            return UniformType.INT_VEC2;
        case GL2ES2.GL_INT_VEC3:
            return UniformType.INT_VEC3;
        case GL2ES2.GL_INT_VEC4:
            return UniformType.INT_VEC4;

        case GL2ES2.GL_BOOL:
            return UniformType.BOOL;

        case GL2GL3.GL_SAMPLER_1D:
            return UniformType.TEXTURE_1D;
        case GL2ES2.GL_SAMPLER_2D:
            return UniformType.TEXTURE_2D;
        case GL2ES2.GL_SAMPLER_3D:
            return UniformType.TEXTURE_3D;
        case GL2ES2.GL_SAMPLER_CUBE:
            return UniformType.TEXTURE_CUBEMAP;
        case GL2ES2.GL_SAMPLER_2D_SHADOW:
            return UniformType.SHADOW_MAP;
        }

        return null;
    }

    /**
     * Return the AttributeType enum value associated with the returned GL2 enum for
     * attribute variable type. Returns null if there's no matching AttributeType.
     */
    public static AttributeType getAttributeType(int type) {
        switch (type) {
        case GL.GL_FLOAT:
            return AttributeType.FLOAT;
        case GL2ES2.GL_FLOAT_VEC2:
            return AttributeType.FLOAT_VEC2;
        case GL2ES2.GL_FLOAT_VEC3:
            return AttributeType.FLOAT_VEC3;
        case GL2ES2.GL_FLOAT_VEC4:
            return AttributeType.FLOAT_VEC4;
        case GL2ES2.GL_FLOAT_MAT2:
            return AttributeType.FLOAT_MAT2;
        case GL2ES2.GL_FLOAT_MAT3:
            return AttributeType.FLOAT_MAT3;
        case GL2ES2.GL_FLOAT_MAT4:
            return AttributeType.FLOAT_MAT4;
        }

        return null;
    }

    /**
     * Return the GL shader type enum for the given type.
     */
    public static int getGLShaderType(ShaderType type) {
        switch (type) {
        case FRAGMENT:
            return GL2ES2.GL_FRAGMENT_SHADER;
        case GEOMETRY:
            return GL3.GL_GEOMETRY_SHADER;
        case VERTEX:
            return GL2ES2.GL_VERTEX_SHADER;
        default:
            return -1;
        }
    }

    /**
     * EffectType can't be null.
     */
    public static int getGLPolygonConnectivity(PolygonType type) {
        switch (type) {
        case LINES:
            return GL.GL_LINES;
        case POINTS:
            return GL.GL_POINTS;
        case QUADS:
            return GL2.GL_QUADS;
        case TRIANGLE_STRIP:
            return GL.GL_TRIANGLE_STRIP;
        case TRIANGLES:
            return GL.GL_TRIANGLES;
        }

        return -1;
    }

    /**
     * Test must not be null.
     */
    public static int getGLPixelTest(Comparison test) {
        switch (test) {
        case ALWAYS:
            return GL.GL_ALWAYS;
        case EQUAL:
            return GL.GL_EQUAL;
        case GEQUAL:
            return GL.GL_GEQUAL;
        case GREATER:
            return GL.GL_GREATER;
        case LEQUAL:
            return GL.GL_LEQUAL;
        case LESS:
            return GL.GL_LESS;
        case NEVER:
            return GL.GL_NEVER;
        case NOT_EQUAL:
            return GL.GL_NOTEQUAL;
        }

        return -1;
    }

    /**
     * Return the gl enum associated with the given filter for minification. filter must
     * not be null.
     */
    public static int getGLMinFilter(Filter filter) {
        switch (filter) {
        case LINEAR:
            return GL.GL_LINEAR;
        case NEAREST:
            return GL.GL_NEAREST;
        case MIPMAP_LINEAR:
            return GL.GL_LINEAR_MIPMAP_LINEAR;
        case MIPMAP_NEAREST:
            return GL.GL_NEAREST_MIPMAP_NEAREST;
        }

        return -1;
    }

    /**
     * Return the gl enum associated with the given filter for magnification. filter must
     * not be null.
     */
    public static int getGLMagFilter(Filter filter) {
        switch (filter) {
        case LINEAR:
        case MIPMAP_LINEAR:
            return GL.GL_LINEAR;
        case NEAREST:
        case MIPMAP_NEAREST:
            return GL.GL_NEAREST;
        }

        return -1;
    }

    /**
     * Wrap must not be null.
     */
    public static int getGLWrapMode(WrapMode wrap) {
        switch (wrap) {
        case CLAMP:
            return GL.GL_CLAMP_TO_EDGE;
        case CLAMP_TO_BORDER:
            return GL2GL3.GL_CLAMP_TO_BORDER;
        case MIRROR:
            return GL.GL_MIRRORED_REPEAT;
        case REPEAT:
            return GL.GL_REPEAT;
        default:
            throw new RuntimeException("Unsupported enum value: " + wrap);
        }
    }

    /**
     * Face must be one of the constants in TextureCubeMap (0 - 5).
     */
    public static int getGLCubeFace(int face) {
        switch (face) {
        case Texture.PX:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
        case Texture.NX:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
        case Texture.PY:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
        case Texture.NY:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
        case Texture.PZ:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
        case Texture.NZ:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }

        return -1;
    }

    /**
     * Target must not be null.
     */
    public static int getGLTextureTarget(Target tar) {
        switch (tar) {
        case T_1D:
            return GL2GL3.GL_TEXTURE_1D;
        case T_2D:
            return GL.GL_TEXTURE_2D;
        case T_3D:
            return GL2ES2.GL_TEXTURE_3D;
        case T_CUBEMAP:
            return GL.GL_TEXTURE_CUBE_MAP;
        }

        return -1;
    }

    /**
     * Format must not be null. Returns an enum for the src format in glTexImage. Returns
     * -1 for compressed formats.
     */
    public static int getGLSrcFormat(TextureFormat format) {
        switch (format) {
        // packed of the RGBA variety (packed type distinguishes them)
        case ABGR_1555:
        case ABGR_4444:
        case ABGR_8888:
            return GL.GL_RGBA;
        case RGBA_4444:
        case RGBA_5551:
        case RGBA_8888:
            return GL.GL_RGBA;

        // packed of the BGRA variety (packed type distinguishes them)
        case ARGB_1555:
        case ARGB_4444:
        case ARGB_8888:
            return GL.GL_BGRA;
        case BGRA_4444:
        case BGRA_5551:
        case BGRA_8888:
            return GL.GL_BGRA;

        // packed and unpacked RGB types
        case RGB:
        case RGB_565:
        case RGB_FLOAT:
            return GL.GL_RGB;

        // packed and unpacked BGR types
        case BGR:
            return GL2GL3.GL_BGR;
        case BGR_565:
            return GL.GL_RGB; // type swaps the ordering

        // unpacked RGBA and BGRA types
        case RGBA:
        case RGBA_FLOAT:
            return GL.GL_RGBA;
        case BGRA:
            return GL.GL_BGRA;

        // depth formats
        case DEPTH:
            return GL2ES2.GL_DEPTH_COMPONENT;

        // red formats
        case R:
        case R_FLOAT:
            return GL.GL_ALPHA;

        // RG formats
        case RG:
        case RG_FLOAT:
            return GL.GL_LUMINANCE_ALPHA;
        default:
            return -1;
        }
    }

    /**
     * Format and type can't be null. Returns an enum for the dst format in glTexImage.
     */
    public static int getGLDstFormat(TextureFormat format, DataType type) {
        switch (format) {
        // packed RGB5_A1
        case ABGR_1555:
        case ARGB_1555:
        case RGBA_5551:
        case BGRA_5551:
            return GL.GL_RGB5_A1;

        // packed RGBA4
        case ABGR_4444:
        case ARGB_4444:
        case RGBA_4444:
        case BGRA_4444:
            return GL.GL_RGBA4;

        // packed RGBA8
        case ABGR_8888:
        case ARGB_8888:
        case RGBA_8888:
        case BGRA_8888:
            return GL.GL_RGBA8;

        // packed RGB8
        case RGB_565:
        case BGR_565:
            return GL2GL3.GL_RGB5;

        // unclamped floating point
        case RGB_FLOAT:
            return GL.GL_RGB32F;
        case RGBA_FLOAT:
            return GL.GL_RGBA32F;
        case R_FLOAT:
            return GL2.GL_ALPHA32F;
        case RG_FLOAT:
            return GL2.GL_LUMINANCE_ALPHA32F;

        // DXT_n compression
        case RGB_DXT1:
            return GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case RGBA_DXT1:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        case RGBA_DXT3:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
        case RGBA_DXT5:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;

        // if we've gotten here, we have a type-less format, and have to
        // take the type into account
        case R:
            if (type == DataType.BYTE) {
                return GL2.GL_ALPHA8;
            } else if (type == DataType.SHORT || type == DataType.INT) {
                return GL2.GL_ALPHA16;
            } else {
                return GL.GL_ALPHA;
            }
        case RG:
            if (type == DataType.BYTE) {
                return GL2.GL_LUMINANCE8_ALPHA8;
            } else if (type == DataType.SHORT || type == DataType.INT) {
                return GL2.GL_LUMINANCE16_ALPHA16;
            } else {
                return GL.GL_LUMINANCE_ALPHA;
            }
        case DEPTH:
            if (type == DataType.BYTE) {
                return GL.GL_DEPTH_COMPONENT16;
            } else if (type == DataType.SHORT) {
                return GL.GL_DEPTH_COMPONENT24;
            } else if (type == DataType.INT) {
                return GL.GL_DEPTH_COMPONENT32;
            } else {
                return GL2ES2.GL_DEPTH_COMPONENT;
            }
        case RGB:
        case BGR:
            if (type == DataType.BYTE) {
                return GL.GL_RGB8;
            } else if (type == DataType.SHORT || type == DataType.INT) {
                return GL2GL3.GL_RGB16;
            } else {
                return GL.GL_RGB;
            }
        case RGBA:
        case BGRA:
            if (type == DataType.BYTE) {
                return GL.GL_RGBA8;
            } else if (type == DataType.SHORT || type == DataType.INT) {
                return GL2GL3.GL_RGBA16;
            } else {
                return GL.GL_RGBA;
            }
        default:
            throw new RuntimeException("Unsupported enum value: " + format);
        }
    }

    /**
     * Format must not be null. Returns an appropriate data type for packed source format.
     * Returns -1 if it's not a packed type. These are chosen with the assumption of
     * big-endian byte ordering.
     */
    public static int getGLPackedType(TextureFormat format) {
        switch (format) {
        // packed ABGR and ARGB types
        case ABGR_1555:
        case ARGB_1555:
            return GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV;
        case ABGR_4444:
        case ARGB_4444:
            return GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV;
        case ABGR_8888:
        case ARGB_8888:
            return GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;

        // packed BGRA and RGBA types
        case BGRA_5551:
        case RGBA_5551:
            return GL.GL_UNSIGNED_SHORT_5_5_5_1;
        case BGRA_4444:
        case RGBA_4444:
            return GL.GL_UNSIGNED_SHORT_4_4_4_4;
        case BGRA_8888:
        case RGBA_8888:
            return GL2GL3.GL_UNSIGNED_INT_8_8_8_8;

        // packed BGR and RGB types
        case BGR_565:
            return GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV;
        case RGB_565:
            return GL.GL_UNSIGNED_SHORT_5_6_5;
        default:
            throw new RuntimeException("Unsupported enum value: " + format);
        }
    }

    /**
     * This shouldn't be used for packed data types
     */
    public static int getGLType(DataType type, boolean signed) {
        switch (type) {
        case FLOAT:
            return GL.GL_FLOAT;
        case BYTE:
            return (signed ? GL.GL_BYTE : GL.GL_UNSIGNED_BYTE);
        case INT:
            return (signed ? GL2.GL_INT : GL.GL_UNSIGNED_INT);
        case SHORT:
            return (signed ? GL2.GL_SHORT : GL.GL_UNSIGNED_SHORT);
        }

        return -1;
    }

    /**
     * Func must not be null.
     */
    public static int getGLBlendEquation(BlendFunction func) {
        switch (func) {
        case ADD:
            return GL.GL_FUNC_ADD;
        case MAX:
            return GL2GL3.GL_MAX;
        case MIN:
            return GL2GL3.GL_MIN;
        case REVERSE_SUBTRACT:
            return GL.GL_FUNC_REVERSE_SUBTRACT;
        case SUBTRACT:
            return GL.GL_FUNC_SUBTRACT;
        }

        return -1;
    }

    /**
     * Src must not be null.
     */
    public static int getGLBlendFactor(BlendFactor src) {
        switch (src) {
        case ZERO:
            return GL.GL_ZERO;
        case ONE:
            return GL.GL_ONE;
        case SRC_COLOR:
            return GL.GL_SRC_COLOR;
        case ONE_MINUS_SRC_COLOR:
            return GL.GL_ONE_MINUS_SRC_COLOR;
        case SRC_ALPHA:
            return GL.GL_SRC_ALPHA;
        case ONE_MINUS_SRC_ALPHA:
            return GL.GL_ONE_MINUS_SRC_ALPHA;
        case SRC_ALPHA_SATURATE:
            return GL.GL_SRC_ALPHA_SATURATE;
        default:
            throw new RuntimeException("Unsupported enum value: " + src);
        }
    }

    /**
     * Should not be called with NONE or null, as no gl enum exists to match it.
     */
    public static int getGLPolygonMode(DrawStyle style) {
        switch (style) {
        case LINE:
            return GL2GL3.GL_LINE;
        case POINT:
            return GL2GL3.GL_POINT;
        case SOLID:
            return GL2GL3.GL_FILL;
        default:
            throw new RuntimeException("Unsupported enum value: " + style);
        }
    }

    /**
     * Op must not be null.
     */
    public static int getGLStencilOp(StencilUpdate op, boolean wrapSupported) {
        switch (op) {
        case DECREMENT:
            return GL.GL_DECR;
        case DECREMENT_WRAP:
            return (wrapSupported ? GL.GL_DECR_WRAP : GL.GL_DECR);
        case INCREMENT:
            return GL.GL_INCR;
        case INCREMENT_WRAP:
            return (wrapSupported ? GL.GL_INCR_WRAP : GL.GL_INCR);
        case ZERO:
            return GL.GL_ZERO;
        case KEEP:
            return GL.GL_KEEP;
        case REPLACE:
            return GL.GL_REPLACE;
        case INVERT:
            return GL.GL_INVERT;
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
            return GL2ES1.GL_REFLECTION_MAP;
        case NORMAL:
            return GL2ES1.GL_NORMAL_MAP;
        default:
            throw new RuntimeException("Unsupported enum value: " + gen);
        }
    }

    /**
     * Coord must be null
     */
    public static int getGLTexCoord(TexCoord coord, boolean forEnable) {
        switch (coord) {
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

    /**
     * Func must not be null.
     */
    public static int getGLCombineFunc(CombineFunction func) {
        switch (func) {
        case ADD:
            return GL2ES1.GL_ADD;
        case ADD_SIGNED:
            return GL2ES1.GL_ADD_SIGNED;
        case DOT3_RGB:
            return GL2ES1.GL_DOT3_RGB;
        case DOT3_RGBA:
            return GL2ES1.GL_DOT3_RGBA;
        case INTERPOLATE:
            return GL2ES1.GL_INTERPOLATE;
        case MODULATE:
            return GL2ES1.GL_MODULATE;
        case REPLACE:
            return GL.GL_REPLACE;
        case SUBTRACT:
            return GL2ES1.GL_SUBTRACT;
        }

        return -1;
    }

    /**
     * Op must not be null.
     */
    public static int getGLCombineOp(CombineOperand op) {
        switch (op) {
        case ALPHA:
            return GL.GL_SRC_ALPHA;
        case COLOR:
            return GL.GL_SRC_COLOR;
        case ONE_MINUS_ALPHA:
            return GL.GL_ONE_MINUS_SRC_ALPHA;
        case ONE_MINUS_COLOR:
            return GL.GL_ONE_MINUS_SRC_COLOR;
        }

        return -1;
    }

    /**
     * Src must not be null.
     */
    public static int getGLCombineSrc(CombineSource src) {
        switch (src) {
        case CONST_COLOR:
            return GL2ES1.GL_CONSTANT;
        case CURR_TEX:
            return GL.GL_TEXTURE;
        case PREV_TEX:
            return GL2ES1.GL_PREVIOUS;
        case VERTEX_COLOR:
            return GL2ES1.GL_PRIMARY_COLOR;
        case TEX0:
            return GL.GL_TEXTURE0;
        case TEX1:
            return GL.GL_TEXTURE1;
        case TEX2:
            return GL.GL_TEXTURE2;
        case TEX3:
            return GL.GL_TEXTURE3;
        case TEX4:
            return GL.GL_TEXTURE4;
        case TEX5:
            return GL.GL_TEXTURE5;
        case TEX6:
            return GL.GL_TEXTURE6;
        case TEX7:
            return GL.GL_TEXTURE7;
        case TEX8:
            return GL.GL_TEXTURE8;
        case TEX9:
            return GL.GL_TEXTURE9;
        case TEX10:
            return GL.GL_TEXTURE10;
        case TEX11:
            return GL.GL_TEXTURE11;
        case TEX12:
            return GL.GL_TEXTURE12;
        case TEX13:
            return GL.GL_TEXTURE13;
        case TEX14:
            return GL.GL_TEXTURE14;
        case TEX15:
            return GL.GL_TEXTURE15;
        case TEX16:
            return GL.GL_TEXTURE16;
        case TEX17:
            return GL.GL_TEXTURE17;
        case TEX18:
            return GL.GL_TEXTURE18;
        case TEX19:
            return GL.GL_TEXTURE19;
        case TEX20:
            return GL.GL_TEXTURE20;
        case TEX21:
            return GL.GL_TEXTURE21;
        case TEX22:
            return GL.GL_TEXTURE22;
        case TEX23:
            return GL.GL_TEXTURE23;
        case TEX24:
            return GL.GL_TEXTURE24;
        case TEX25:
            return GL.GL_TEXTURE25;
        case TEX26:
            return GL.GL_TEXTURE26;
        case TEX27:
            return GL.GL_TEXTURE27;
        case TEX28:
            return GL.GL_TEXTURE28;
        case TEX29:
            return GL.GL_TEXTURE29;
        case TEX30:
            return GL.GL_TEXTURE30;
        case TEX31:
            return GL.GL_TEXTURE31;
        }

        return -1;
    }

    /**
     * Utility method to invoke a Runnable on the context-thread of the given framework.
     * This will throw a runtime exception if a problem occurs. It works properly if
     * called from the context thread. This should be used when EventQueue.invokeAndWait()
     * or SwingUtilities.invokeAndWait() would be used, except that this is thread safe.
     */
    public static void invokeOnContextThread(ContextManager cm, final Runnable r,
                                             boolean block) {
        if (cm.isContextThread()) {
            r.run();
        } else {
            Future<Void> task = cm.invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() {
                    r.run();
                    return null;
                }
            }, false);
            if (block) {
                try {
                    task.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

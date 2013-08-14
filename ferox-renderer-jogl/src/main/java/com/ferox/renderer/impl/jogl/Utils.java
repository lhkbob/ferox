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

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.DataType;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOperand;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.ContextManager;
import com.ferox.renderer.impl.resources.TextureImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2GL3;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utils provides conversions for the commonly used enums in Resources and Renderers to their appropriate GL
 * enum values, as well as some other useful utilities.
 *
 * @author Michael Ludwig
 */
public class Utils {
    /**
     * Return the VariableType enum value associated with the returned GL enum for glsl variable type.
     */
    public static Shader.VariableType getVariableType(int type) {
        switch (type) {
        case GL.GL_FLOAT:
            return Shader.VariableType.FLOAT;
        case GL2GL3.GL_FLOAT_VEC2:
            return Shader.VariableType.VEC2;
        case GL2GL3.GL_FLOAT_VEC3:
            return Shader.VariableType.VEC3;
        case GL2GL3.GL_FLOAT_VEC4:
            return Shader.VariableType.VEC4;

        case GL2GL3.GL_FLOAT_MAT2:
            return Shader.VariableType.MAT2;
        case GL2GL3.GL_FLOAT_MAT3:
            return Shader.VariableType.MAT3;
        case GL2GL3.GL_FLOAT_MAT4:
            return Shader.VariableType.MAT4;

        case GL2GL3.GL_INT:
            return Shader.VariableType.INT;
        case GL2GL3.GL_INT_VEC2:
            return Shader.VariableType.IVEC2;
        case GL2GL3.GL_INT_VEC3:
            return Shader.VariableType.IVEC3;
        case GL2GL3.GL_INT_VEC4:
            return Shader.VariableType.IVEC4;

        case GL.GL_UNSIGNED_INT:
            return Shader.VariableType.UINT;
        case GL2GL3.GL_UNSIGNED_INT_VEC2:
            return Shader.VariableType.IVEC2;
        case GL2GL3.GL_UNSIGNED_INT_VEC3:
            return Shader.VariableType.IVEC3;
        case GL2GL3.GL_UNSIGNED_INT_VEC4:
            return Shader.VariableType.IVEC4;

        case GL2GL3.GL_BOOL:
            return Shader.VariableType.BOOL;
        case GL2GL3.GL_BOOL_VEC2:
            return Shader.VariableType.BVEC2;
        case GL2GL3.GL_BOOL_VEC3:
            return Shader.VariableType.BVEC3;
        case GL2GL3.GL_BOOL_VEC4:
            return Shader.VariableType.BVEC4;

        case GL2GL3.GL_SAMPLER_1D:
            return Shader.VariableType.SAMPLER_1D;
        case GL2GL3.GL_SAMPLER_2D:
            return Shader.VariableType.SAMPLER_2D;
        case GL2GL3.GL_SAMPLER_3D:
            return Shader.VariableType.SAMPLER_3D;
        case GL2GL3.GL_SAMPLER_CUBE:
            return Shader.VariableType.SAMPLER_CUBE;
        case GL2GL3.GL_SAMPLER_2D_SHADOW:
            return Shader.VariableType.SAMPLER_2D_SHADOW;
        case GL2GL3.GL_SAMPLER_CUBE_SHADOW:
            return Shader.VariableType.SAMPLER_CUBE_SHADOW;
        case GL2GL3.GL_SAMPLER_1D_ARRAY:
            return Shader.VariableType.SAMPLER_1D_ARRAY;
        case GL2GL3.GL_SAMPLER_2D_ARRAY:
            return Shader.VariableType.SAMPLER_2D_ARRAY;

        case GL2GL3.GL_INT_SAMPLER_1D:
            return Shader.VariableType.ISAMPLER_1D;
        case GL2GL3.GL_INT_SAMPLER_2D:
            return Shader.VariableType.ISAMPLER_2D;
        case GL2GL3.GL_INT_SAMPLER_3D:
            return Shader.VariableType.ISAMPLER_3D;
        case GL2GL3.GL_INT_SAMPLER_CUBE:
            return Shader.VariableType.ISAMPLER_CUBE;
        case GL2GL3.GL_INT_SAMPLER_1D_ARRAY:
            return Shader.VariableType.ISAMPLER_1D_ARRAY;
        case GL2GL3.GL_INT_SAMPLER_2D_ARRAY:
            return Shader.VariableType.ISAMPLER_2D_ARRAY;

        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_1D:
            return Shader.VariableType.ISAMPLER_1D;
        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_2D:
            return Shader.VariableType.ISAMPLER_2D;
        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_3D:
            return Shader.VariableType.ISAMPLER_3D;
        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_CUBE:
            return Shader.VariableType.ISAMPLER_CUBE;
        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY:
            return Shader.VariableType.ISAMPLER_1D_ARRAY;
        case GL2GL3.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY:
            return Shader.VariableType.ISAMPLER_2D_ARRAY;
        }

        return null;
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
     * Return the gl enum associated with the given filter for minification.
     */
    public static int getGLMinFilter(boolean interpolate, boolean hasMipmaps) {
        if (interpolate) {
            if (hasMipmaps) {
                return GL.GL_LINEAR_MIPMAP_LINEAR;
            } else {
                return GL.GL_LINEAR;
            }
        } else {
            if (hasMipmaps) {
                return GL.GL_NEAREST_MIPMAP_NEAREST;
            } else {
                return GL.GL_NEAREST;
            }
        }
    }

    /**
     * Return the gl enum associated with the given filter for magnification. filter must not be null.
     */
    public static int getGLMagFilter(boolean interpolate) {
        return interpolate ? GL.GL_LINEAR : GL.GL_NEAREST;
    }

    /**
     * Wrap must not be null.
     */
    public static int getGLWrapMode(Sampler.WrapMode wrap) {
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
        case TextureImpl.POSITIVE_X:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
        case TextureImpl.NEGATIVE_X:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
        case TextureImpl.POSITIVE_Y:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
        case TextureImpl.NEGATIVE_Y:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
        case TextureImpl.POSITIVE_Z:
            return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
        case TextureImpl.NEGATIVE_Z:
            return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }

        return -1;
    }

    /**
     * Target must not be null.
     */
    public static int getGLTextureTarget(TextureImpl.Target tar) {
        switch (tar) {
        case TEX_1D:
            return GL2GL3.GL_TEXTURE_1D;
        case TEX_2D:
            return GL2GL3.GL_TEXTURE_2D;
        case TEX_3D:
            return GL2GL3.GL_TEXTURE_3D;
        case TEX_CUBEMAP:
            return GL2GL3.GL_TEXTURE_CUBE_MAP;
        case TEX_1D_ARRAY:
            return GL2GL3.GL_TEXTURE_1D_ARRAY;
        case TEX_2D_ARRAY:
            return GL2GL3.GL_TEXTURE_2D_ARRAY;
        }

        return -1;
    }

    public static int getGLSrcFormat(TextureImpl.FullFormat format, Capabilities caps) {
        switch (format) {
        case DEPTH_24BIT:
        case DEPTH_16BIT:
        case DEPTH_FLOAT:
            return GL2GL3.GL_DEPTH_COMPONENT;
        case DEPTH_24BIT_STENCIL_8BIT:
            return GL2GL3.GL_DEPTH_STENCIL;
        case R_BYTE:
        case R_SHORT:
        case R_INT:
        case R_UBYTE:
        case R_USHORT:
        case R_UINT:
            return GL2GL3.GL_RED_INTEGER;
        case R_FLOAT:
        case R_NORMALIZED_UBYTE:
        case R_NORMALIZED_USHORT:
        case R_NORMALIZED_UINT:
        case R_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE;
            } else {
                // This doesn't get promoted to a valid format until 3.0
                return GL2GL3.GL_RED;
            }
        case RG_BYTE:
        case RG_SHORT:
        case RG_INT:
        case RG_UBYTE:
        case RG_USHORT:
        case RG_UINT:
            return GL2GL3.GL_RG_INTEGER;
        case RG_FLOAT:
        case RG_NORMALIZED_UBYTE:
        case RG_NORMALIZED_USHORT:
        case RG_NORMALIZED_UINT:
        case RG_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE_ALPHA;
            } else {
                return GL2GL3.GL_RG;
            }
        case RGB_BYTE:
        case RGB_SHORT:
        case RGB_INT:
        case RGB_UBYTE:
        case RGB_USHORT:
        case RGB_UINT:
            return GL2GL3.GL_RGB_INTEGER;
        case RGB_FLOAT:
        case RGB_NORMALIZED_UBYTE:
        case RGB_NORMALIZED_USHORT:
        case RGB_NORMALIZED_UINT:
        case RGB_HALF_FLOAT:
        case RGB_PACKED_FLOAT:
            return GL2GL3.GL_RGB;
        case BGR_BYTE:
        case BGR_SHORT:
        case BGR_INT:
        case BGR_UBYTE:
        case BGR_USHORT:
        case BGR_UINT:
            return GL2GL3.GL_BGR_INTEGER;
        case BGR_FLOAT:
        case BGR_NORMALIZED_UBYTE:
        case BGR_NORMALIZED_USHORT:
        case BGR_NORMALIZED_UINT:
        case BGR_HALF_FLOAT:
            return GL2GL3.GL_BGR;
        case RGBA_BYTE:
        case RGBA_SHORT:
        case RGBA_INT:
        case RGBA_UBYTE:
        case RGBA_USHORT:
        case RGBA_UINT:
            return GL2GL3.GL_RGBA_INTEGER;
        case RGBA_FLOAT:
        case RGBA_NORMALIZED_UBYTE:
        case RGBA_NORMALIZED_USHORT:
        case RGBA_NORMALIZED_UINT:
        case RGBA_HALF_FLOAT:
            return GL2GL3.GL_RGBA;
        case RGB_DXT1:
        case RGBA_DXT1:
        case RGBA_DXT3:
        case RGBA_DXT5:
            return -1; // no src format for compressed types
        case BGRA_BYTE:
        case BGRA_SHORT:
        case BGRA_INT:
        case BGRA_UBYTE:
        case BGRA_USHORT:
        case BGRA_UINT:
            return GL2GL3.GL_BGRA_INTEGER;
        case BGRA_FLOAT:
        case BGRA_NORMALIZED_UBYTE:
        case BGRA_NORMALIZED_USHORT:
        case BGRA_NORMALIZED_UINT:
        case BGRA_HALF_FLOAT:
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL.GL_BGRA;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    public static int getGLDstFormat(TextureImpl.FullFormat format, Capabilities caps) {
        switch (format) {
        case DEPTH_24BIT:
            return GL.GL_DEPTH_COMPONENT24;
        case DEPTH_16BIT:
            return GL.GL_DEPTH_COMPONENT16;
        case DEPTH_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL2GL3.GL_DEPTH_COMPONENT;
            } else {
                return GL2GL3.GL_DEPTH_COMPONENT32F;
            }
        case DEPTH_24BIT_STENCIL_8BIT:
            return GL2GL3.GL_DEPTH24_STENCIL8;
        case R_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL2.GL_LUMINANCE16;
            } else if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE32F_ARB;
            } else {
                return GL2GL3.GL_R32F;
            }
        case R_BYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE8I;
            } else {
                return GL2GL3.GL_R8I;
            }
        case R_SHORT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE16I;
            } else {
                return GL2GL3.GL_R16I;
            }
        case R_INT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE32I;
            } else {
                return GL2GL3.GL_R32I;
            }
        case R_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE8UI;
            } else {
                return GL2GL3.GL_R8UI;
            }
        case R_USHORT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE16UI;
            } else {
                return GL2GL3.GL_R16UI;
            }
        case R_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE32UI;
            } else {
                return GL2GL3.GL_R32UI;
            }
        case R_NORMALIZED_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE8;
            } else {
                return GL2GL3.GL_R8;
            }
        case R_NORMALIZED_USHORT:
        case R_NORMALIZED_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE16;
            } else {
                return GL2GL3.GL_R16;
            }
        case R_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE16F_ARB;
            } else {
                return GL2GL3.GL_R16F;
            }
        case RG_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL2.GL_LUMINANCE16_ALPHA16;
            } else if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE_ALPHA32F_ARB;
            } else {
                return GL2GL3.GL_RG32F;
            }
        case RG_BYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA8I;
            } else {
                return GL2GL3.GL_RG8I;
            }
        case RG_SHORT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA16I;
            } else {
                return GL2GL3.GL_RG16I;
            }
        case RG_INT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA32I;
            } else {
                return GL2GL3.GL_RG32I;
            }
        case RG_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA8UI;
            } else {
                return GL2GL3.GL_RG8UI;
            }
        case RG_USHORT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA16UI;
            } else {
                return GL2GL3.GL_RG16UI;
            }
        case RG_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE_ALPHA32UI;
            } else {
                return GL2GL3.GL_RG32UI;
            }
        case RG_NORMALIZED_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE8_ALPHA8;
            } else {
                return GL2GL3.GL_RG8;
            }
        case RG_NORMALIZED_USHORT:
        case RG_NORMALIZED_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL2.GL_LUMINANCE16_ALPHA16;
            } else {
                return GL2GL3.GL_RG16;
            }
        case RG_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL2GL3.GL_LUMINANCE_ALPHA16F_ARB;
            } else {
                return GL2GL3.GL_RG16F;
            }
        case RGB_FLOAT:
        case BGR_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL2GL3.GL_RGB16;
            } else {
                return GL2GL3.GL_RGB32F;
            }
        case RGB_BYTE:
        case BGR_BYTE:
            return GL2GL3.GL_RGB8I;
        case RGB_SHORT:
        case BGR_SHORT:
            return GL2GL3.GL_RGB16I;
        case RGB_INT:
        case BGR_INT:
            return GL2GL3.GL_RGB32I;
        case RGB_UBYTE:
        case BGR_UBYTE:
            return GL2GL3.GL_RGB8UI;
        case RGB_USHORT:
        case BGR_USHORT:
            return GL2GL3.GL_RGB16UI;
        case RGB_UINT:
        case BGR_UINT:
            return GL2GL3.GL_RGB32UI;
        case RGB_NORMALIZED_UBYTE:
        case BGR_NORMALIZED_UBYTE:
            return GL2GL3.GL_RGB8;
        case RGB_NORMALIZED_USHORT:
        case RGB_NORMALIZED_UINT:
        case BGR_NORMALIZED_USHORT:
        case BGR_NORMALIZED_UINT:
            return GL2GL3.GL_RGB16;
        case RGB_HALF_FLOAT:
        case BGR_HALF_FLOAT:
            return GL2GL3.GL_RGB16F;
        case RGB_PACKED_FLOAT:
            return GL2GL3.GL_R11F_G11F_B10F;
        case RGB_DXT1:
            return GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case RGBA_FLOAT:
        case BGRA_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL2GL3.GL_RGBA16;
            } else {
                return GL2GL3.GL_RGB32F;
            }
        case RGBA_BYTE:
        case BGRA_BYTE:
            return GL2GL3.GL_RGB8I;
        case RGBA_SHORT:
        case BGRA_SHORT:
            return GL2GL3.GL_RGB16I;
        case RGBA_INT:
        case BGRA_INT:
            return GL2GL3.GL_RGB32I;
        case RGBA_UBYTE:
        case BGRA_UBYTE:
            return GL2GL3.GL_RGB8UI;
        case RGBA_USHORT:
        case BGRA_USHORT:
            return GL2GL3.GL_RGB16UI;
        case RGBA_UINT:
        case BGRA_UINT:
            return GL2GL3.GL_RGB32UI;
        case RGBA_NORMALIZED_UBYTE:
        case BGRA_NORMALIZED_UBYTE:
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL2GL3.GL_RGBA8;
        case RGBA_NORMALIZED_USHORT:
        case RGBA_NORMALIZED_UINT:
        case BGRA_NORMALIZED_USHORT:
        case BGRA_NORMALIZED_UINT:
            return GL2GL3.GL_RGBA16;
        case RGBA_HALF_FLOAT:
        case BGRA_HALF_FLOAT:
            return GL2GL3.GL_RGBA16F;
        case RGBA_DXT1:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        case RGBA_DXT3:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
        case RGBA_DXT5:
            return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    public static int getGLDataTypeForPackedTextureFormat(TextureImpl.FullFormat format) {
        switch (format) {
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;
        case DEPTH_24BIT_STENCIL_8BIT:
            return GL2GL3.GL_UNSIGNED_INT_24_8;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    /**
     * This shouldn't be used for packed data types
     */
    public static int getGLType(DataType type) {
        switch (type) {
        case FLOAT:
            return GL.GL_FLOAT;
        case HALF_FLOAT:
            return GL2.GL_HALF_FLOAT;
        case INT:
        case NORMALIZED_INT: // normalization is determined by the use case
            return GL2.GL_INT;
        case UNSIGNED_INT:
        case UNSIGNED_NORMALIZED_INT:
        case INT_BIT_FIELD: // best match here, more specific values are determined by use case
            return GL.GL_UNSIGNED_INT;
        case SHORT:
        case NORMALIZED_SHORT:
            return GL.GL_SHORT;
        case UNSIGNED_SHORT:
        case UNSIGNED_NORMALIZED_SHORT:
            return GL.GL_UNSIGNED_SHORT;
        case BYTE:
        case NORMALIZED_BYTE:
            return GL.GL_BYTE;
        case UNSIGNED_BYTE:
        case UNSIGNED_NORMALIZED_BYTE:
            return GL.GL_UNSIGNED_BYTE;
        default:
            return -1;
        }
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
    public static int getGLStencilOp(StencilUpdate op) {
        switch (op) {
        case DECREMENT:
            return GL.GL_DECR;
        case DECREMENT_WRAP:
            return GL.GL_DECR_WRAP;
        case INCREMENT:
            return GL.GL_INCR;
        case INCREMENT_WRAP:
            return GL.GL_INCR_WRAP;
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
     * Utility method to invoke a Runnable on the context-thread of the given framework. This will throw a
     * runtime exception if a problem occurs. It works properly if called from the context thread. This should
     * be used when EventQueue.invokeAndWait() or SwingUtilities.invokeAndWait() would be used, except that
     * this is thread safe.
     */
    public static void invokeOnContextThread(ContextManager cm, final Runnable r, boolean block) {
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

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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.DataType;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOperand;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.opengl.*;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

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
        case GL11.GL_FLOAT:
            return Shader.VariableType.FLOAT;
        case GL20.GL_FLOAT_VEC2:
            return Shader.VariableType.VEC2;
        case GL20.GL_FLOAT_VEC3:
            return Shader.VariableType.VEC3;
        case GL20.GL_FLOAT_VEC4:
            return Shader.VariableType.VEC4;

        case GL20.GL_FLOAT_MAT2:
            return Shader.VariableType.MAT2;
        case GL20.GL_FLOAT_MAT3:
            return Shader.VariableType.MAT3;
        case GL20.GL_FLOAT_MAT4:
            return Shader.VariableType.MAT4;

        case GL11.GL_INT:
            return Shader.VariableType.INT;
        case GL20.GL_INT_VEC2:
            return Shader.VariableType.IVEC2;
        case GL20.GL_INT_VEC3:
            return Shader.VariableType.IVEC3;
        case GL20.GL_INT_VEC4:
            return Shader.VariableType.IVEC4;

        case GL11.GL_UNSIGNED_INT:
            return Shader.VariableType.UINT;
        case GL30.GL_UNSIGNED_INT_VEC2:
            return Shader.VariableType.IVEC2;
        case GL30.GL_UNSIGNED_INT_VEC3:
            return Shader.VariableType.IVEC3;
        case GL30.GL_UNSIGNED_INT_VEC4:
            return Shader.VariableType.IVEC4;

        case GL20.GL_BOOL:
            return Shader.VariableType.BOOL;
        case GL20.GL_BOOL_VEC2:
            return Shader.VariableType.BVEC2;
        case GL20.GL_BOOL_VEC3:
            return Shader.VariableType.BVEC3;
        case GL20.GL_BOOL_VEC4:
            return Shader.VariableType.BVEC4;

        case GL20.GL_SAMPLER_1D:
            return Shader.VariableType.SAMPLER_1D;
        case GL20.GL_SAMPLER_2D:
            return Shader.VariableType.SAMPLER_2D;
        case GL20.GL_SAMPLER_3D:
            return Shader.VariableType.SAMPLER_3D;
        case GL20.GL_SAMPLER_CUBE:
            return Shader.VariableType.SAMPLER_CUBE;
        case GL20.GL_SAMPLER_2D_SHADOW:
            return Shader.VariableType.SAMPLER_2D_SHADOW;
        case GL30.GL_SAMPLER_CUBE_SHADOW:
            return Shader.VariableType.SAMPLER_CUBE_SHADOW;
        case GL30.GL_SAMPLER_1D_ARRAY:
            return Shader.VariableType.SAMPLER_1D_ARRAY;
        case GL30.GL_SAMPLER_2D_ARRAY:
            return Shader.VariableType.SAMPLER_2D_ARRAY;

        case GL30.GL_INT_SAMPLER_1D:
            return Shader.VariableType.ISAMPLER_1D;
        case GL30.GL_INT_SAMPLER_2D:
            return Shader.VariableType.ISAMPLER_2D;
        case GL30.GL_INT_SAMPLER_3D:
            return Shader.VariableType.ISAMPLER_3D;
        case GL30.GL_INT_SAMPLER_CUBE:
            return Shader.VariableType.ISAMPLER_CUBE;
        case GL30.GL_INT_SAMPLER_1D_ARRAY:
            return Shader.VariableType.ISAMPLER_1D_ARRAY;
        case GL30.GL_INT_SAMPLER_2D_ARRAY:
            return Shader.VariableType.ISAMPLER_2D_ARRAY;

        case GL30.GL_UNSIGNED_INT_SAMPLER_1D:
            return Shader.VariableType.ISAMPLER_1D;
        case GL30.GL_UNSIGNED_INT_SAMPLER_2D:
            return Shader.VariableType.ISAMPLER_2D;
        case GL30.GL_UNSIGNED_INT_SAMPLER_3D:
            return Shader.VariableType.ISAMPLER_3D;
        case GL30.GL_UNSIGNED_INT_SAMPLER_CUBE:
            return Shader.VariableType.ISAMPLER_CUBE;
        case GL30.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY:
            return Shader.VariableType.ISAMPLER_1D_ARRAY;
        case GL30.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY:
            return Shader.VariableType.ISAMPLER_2D_ARRAY;
        }

        return null;
    }

    /**
     * PolygonType can't be null.
     */
    public static int getGLPolygonConnectivity(PolygonType type) {
        switch (type) {
        case LINES:
            return GL11.GL_LINES;
        case POINTS:
            return GL11.GL_POINTS;
        case TRIANGLE_STRIP:
            return GL11.GL_TRIANGLE_STRIP;
        case TRIANGLES:
            return GL11.GL_TRIANGLES;
        }

        return -1;
    }

    /**
     * Test must not be null.
     */
    public static int getGLPixelTest(Comparison test) {
        switch (test) {
        case ALWAYS:
            return GL11.GL_ALWAYS;
        case EQUAL:
            return GL11.GL_EQUAL;
        case GEQUAL:
            return GL11.GL_GEQUAL;
        case GREATER:
            return GL11.GL_GREATER;
        case LEQUAL:
            return GL11.GL_LEQUAL;
        case LESS:
            return GL11.GL_LESS;
        case NEVER:
            return GL11.GL_NEVER;
        case NOT_EQUAL:
            return GL11.GL_NOTEQUAL;
        }

        return -1;
    }

    /**
     * Return the gl enum associated with the given filter for minification.
     */
    public static int getGLMinFilter(boolean interpolate, boolean hasMipmaps) {
        if (interpolate) {
            if (hasMipmaps) {
                return GL11.GL_LINEAR_MIPMAP_LINEAR;
            } else {
                return GL11.GL_LINEAR;
            }
        } else {
            if (hasMipmaps) {
                return GL11.GL_NEAREST_MIPMAP_NEAREST;
            } else {
                return GL11.GL_NEAREST;
            }
        }
    }

    /**
     * Return the gl enum associated with the given filter for magnification. filter must not be null.
     */
    public static int getGLMagFilter(boolean interpolate) {
        return interpolate ? GL11.GL_LINEAR : GL11.GL_NEAREST;
    }

    /**
     * Wrap must not be null.
     */
    public static int getGLWrapMode(Sampler.WrapMode wrap) {
        switch (wrap) {
        case CLAMP:
            return GL12.GL_CLAMP_TO_EDGE;
        case CLAMP_TO_BORDER:
            return GL13.GL_CLAMP_TO_BORDER;
        case MIRROR:
            return GL14.GL_MIRRORED_REPEAT;
        case REPEAT:
            return GL11.GL_REPEAT;
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
            return GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
        case TextureImpl.NEGATIVE_X:
            return GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
        case TextureImpl.POSITIVE_Y:
            return GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
        case TextureImpl.NEGATIVE_Y:
            return GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
        case TextureImpl.POSITIVE_Z:
            return GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
        case TextureImpl.NEGATIVE_Z:
            return GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }

        return -1;
    }

    /**
     * Target must not be null.
     */
    public static int getGLTextureTarget(TextureImpl.Target tar) {
        switch (tar) {
        case TEX_1D:
            return GL11.GL_TEXTURE_1D;
        case TEX_2D:
            return GL11.GL_TEXTURE_2D;
        case TEX_3D:
            return GL12.GL_TEXTURE_3D;
        case TEX_CUBEMAP:
            return GL13.GL_TEXTURE_CUBE_MAP;
        case TEX_1D_ARRAY:
            return GL30.GL_TEXTURE_1D_ARRAY;
        case TEX_2D_ARRAY:
            return GL30.GL_TEXTURE_2D_ARRAY;
        }

        return -1;
    }

    public static int getGLSrcFormat(TextureImpl.FullFormat format, Capabilities caps) {
        switch (format) {
        case DEPTH_24BIT:
        case DEPTH_16BIT:
        case DEPTH_FLOAT:
            return GL11.GL_DEPTH_COMPONENT;
        case DEPTH_24BIT_STENCIL_8BIT:
            return GL30.GL_DEPTH_STENCIL;
        case R_BYTE:
        case R_SHORT:
        case R_INT:
        case R_UBYTE:
        case R_USHORT:
        case R_UINT:
            return GL30.GL_RED_INTEGER;
        case R_FLOAT:
        case R_NORMALIZED_UBYTE:
        case R_NORMALIZED_USHORT:
        case R_NORMALIZED_UINT:
        case R_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE;
            } else {
                // This doesn't get promoted to a valid format until 3.0
                return GL11.GL_RED;
            }
        case RG_BYTE:
        case RG_SHORT:
        case RG_INT:
        case RG_UBYTE:
        case RG_USHORT:
        case RG_UINT:
            return GL30.GL_RG_INTEGER;
        case RG_FLOAT:
        case RG_NORMALIZED_UBYTE:
        case RG_NORMALIZED_USHORT:
        case RG_NORMALIZED_UINT:
        case RG_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE_ALPHA;
            } else {
                return GL30.GL_RG;
            }
        case RGB_BYTE:
        case RGB_SHORT:
        case RGB_INT:
        case RGB_UBYTE:
        case RGB_USHORT:
        case RGB_UINT:
            return GL30.GL_RGB_INTEGER;
        case RGB_FLOAT:
        case RGB_NORMALIZED_UBYTE:
        case RGB_NORMALIZED_USHORT:
        case RGB_NORMALIZED_UINT:
        case RGB_HALF_FLOAT:
        case RGB_PACKED_FLOAT:
            return GL11.GL_RGB;
        case BGR_BYTE:
        case BGR_SHORT:
        case BGR_INT:
        case BGR_UBYTE:
        case BGR_USHORT:
        case BGR_UINT:
            return GL30.GL_BGR_INTEGER;
        case BGR_FLOAT:
        case BGR_NORMALIZED_UBYTE:
        case BGR_NORMALIZED_USHORT:
        case BGR_NORMALIZED_UINT:
        case BGR_HALF_FLOAT:
            return GL12.GL_BGR;
        case RGBA_BYTE:
        case RGBA_SHORT:
        case RGBA_INT:
        case RGBA_UBYTE:
        case RGBA_USHORT:
        case RGBA_UINT:
            return GL30.GL_RGBA_INTEGER;
        case RGBA_FLOAT:
        case RGBA_NORMALIZED_UBYTE:
        case RGBA_NORMALIZED_USHORT:
        case RGBA_NORMALIZED_UINT:
        case RGBA_HALF_FLOAT:
            return GL11.GL_RGBA;
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
            return GL30.GL_BGRA_INTEGER;
        case BGRA_FLOAT:
        case BGRA_NORMALIZED_UBYTE:
        case BGRA_NORMALIZED_USHORT:
        case BGRA_NORMALIZED_UINT:
        case BGRA_HALF_FLOAT:
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL12.GL_BGRA;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    public static int getGLDstFormat(TextureImpl.FullFormat format, Capabilities caps) {
        switch (format) {
        case DEPTH_24BIT:
            return GL14.GL_DEPTH_COMPONENT24;
        case DEPTH_16BIT:
            return GL14.GL_DEPTH_COMPONENT16;
        case DEPTH_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL11.GL_DEPTH_COMPONENT;
            } else if (caps.getMajorVersion() < 3) {
                return ARBDepthBufferFloat.GL_DEPTH_COMPONENT32F;
            } else {
                return GL30.GL_DEPTH_COMPONENT32F;
            }
        case DEPTH_24BIT_STENCIL_8BIT:
            if (caps.getMajorVersion() < 3) {
                return EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT;
            } else {
                return GL30.GL_DEPTH24_STENCIL8;
            }
        case R_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL11.GL_LUMINANCE16;
            } else if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_LUMINANCE32F_ARB;
            } else {
                return GL30.GL_R32F;
            }
        case R_BYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE8I_EXT;
            } else {
                return GL30.GL_R8I;
            }
        case R_SHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE16I_EXT;
            } else {
                return GL30.GL_R16I;
            }
        case R_INT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE32I_EXT;
            } else {
                return GL30.GL_R32I;
            }
        case R_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE8UI_EXT;
            } else {
                return GL30.GL_R8UI;
            }
        case R_USHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE16UI_EXT;
            } else {
                return GL30.GL_R16UI;
            }
        case R_UINT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE32UI_EXT;
            } else {
                return GL30.GL_R32UI;
            }
        case R_NORMALIZED_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE8;
            } else {
                return GL30.GL_R8;
            }
        case R_NORMALIZED_USHORT:
        case R_NORMALIZED_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE16;
            } else {
                return GL30.GL_R16;
            }
        case R_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_LUMINANCE16F_ARB;
            } else {
                return GL30.GL_R16F;
            }
        case RG_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL11.GL_LUMINANCE16_ALPHA16;
            } else if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_LUMINANCE_ALPHA32F_ARB;
            } else {
                return GL30.GL_RG32F;
            }
        case RG_BYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA8I_EXT;
            } else {
                return GL30.GL_RG8I;
            }
        case RG_SHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA16I_EXT;
            } else {
                return GL30.GL_RG16I;
            }
        case RG_INT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA32I_EXT;
            } else {
                return GL30.GL_RG32I;
            }
        case RG_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA8UI_EXT;
            } else {
                return GL30.GL_RG8UI;
            }
        case RG_USHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA16UI_EXT;
            } else {
                return GL30.GL_RG16UI;
            }
        case RG_UINT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_LUMINANCE_ALPHA32UI_EXT;
            } else {
                return GL30.GL_RG32UI;
            }
        case RG_NORMALIZED_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE8_ALPHA8;
            } else {
                return GL30.GL_RG8;
            }
        case RG_NORMALIZED_USHORT:
        case RG_NORMALIZED_UINT:
            if (caps.getMajorVersion() < 3) {
                return GL11.GL_LUMINANCE16_ALPHA16;
            } else {
                return GL30.GL_RG16;
            }
        case RG_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_LUMINANCE_ALPHA16F_ARB;
            } else {
                return GL30.GL_RG16F;
            }
        case RGB_FLOAT:
        case BGR_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL11.GL_RGB16;
            } else if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_RGB32F_ARB;
            } else {
                return GL30.GL_RGB32F;
            }
        case RGB_BYTE:
        case BGR_BYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB8I_EXT;
            } else {
                return GL30.GL_RGB8I;
            }
        case RGB_SHORT:
        case BGR_SHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB16I_EXT;
            } else {
                return GL30.GL_RGB16I;
            }
        case RGB_INT:
        case BGR_INT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB32I_EXT;
            } else {
                return GL30.GL_RGB32I;
            }
        case RGB_UBYTE:
        case BGR_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB8UI_EXT;
            } else {
                return GL30.GL_RGB8UI;
            }
        case RGB_USHORT:
        case BGR_USHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB16UI_EXT;
            } else {
                return GL30.GL_RGB16UI;
            }
        case RGB_UINT:
        case BGR_UINT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGB32UI_EXT;
            } else {
                return GL30.GL_RGB32UI;
            }
        case RGB_NORMALIZED_UBYTE:
        case BGR_NORMALIZED_UBYTE:
            return GL11.GL_RGB8;
        case RGB_NORMALIZED_USHORT:
        case RGB_NORMALIZED_UINT:
        case BGR_NORMALIZED_USHORT:
        case BGR_NORMALIZED_UINT:
            return GL11.GL_RGB16;
        case RGB_HALF_FLOAT:
        case BGR_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_RGB16F_ARB;
            } else {
                return GL30.GL_RGB16F;
            }
        case RGB_PACKED_FLOAT:
            return GL30.GL_R11F_G11F_B10F;
        case RGB_DXT1:
            return EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case RGBA_FLOAT:
        case BGRA_FLOAT:
            if (!caps.getUnclampedFloatTextureSupport()) {
                return GL11.GL_RGBA16;
            } else if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_RGBA32F_ARB;
            } else {
                return GL30.GL_RGB32F;
            }
        case RGBA_BYTE:
        case BGRA_BYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA8I_EXT;
            } else {
                return GL30.GL_RGB8I;
            }
        case RGBA_SHORT:
        case BGRA_SHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA16I_EXT;
            } else {
                return GL30.GL_RGB16I;
            }
        case RGBA_INT:
        case BGRA_INT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA32I_EXT;
            } else {
                return GL30.GL_RGB32I;
            }
        case RGBA_UBYTE:
        case BGRA_UBYTE:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA8UI_EXT;
            } else {
                return GL30.GL_RGB8UI;
            }
        case RGBA_USHORT:
        case BGRA_USHORT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA16UI_EXT;
            } else {
                return GL30.GL_RGB16UI;
            }
        case RGBA_UINT:
        case BGRA_UINT:
            if (caps.getMajorVersion() < 3) {
                return EXTTextureInteger.GL_RGBA32UI_EXT;
            } else {
                return GL30.GL_RGB32UI;
            }
        case RGBA_NORMALIZED_UBYTE:
        case BGRA_NORMALIZED_UBYTE:
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL11.GL_RGBA8;
        case RGBA_NORMALIZED_USHORT:
        case RGBA_NORMALIZED_UINT:
        case BGRA_NORMALIZED_USHORT:
        case BGRA_NORMALIZED_UINT:
            return GL11.GL_RGBA16;
        case RGBA_HALF_FLOAT:
        case BGRA_HALF_FLOAT:
            if (caps.getMajorVersion() < 3) {
                return ARBTextureFloat.GL_RGBA16F_ARB;
            } else {
                return GL30.GL_RGBA16F;
            }
        case RGBA_DXT1:
            return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        case RGBA_DXT3:
            return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
        case RGBA_DXT5:
            return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    public static int getGLDataTypeForPackedTextureFormat(TextureImpl.FullFormat format) {
        switch (format) {
        case ARGB_NORMALIZED_UBYTE:
        case ARGB_PACKED_INT:
            return GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
        case DEPTH_24BIT_STENCIL_8BIT:
            return GL30.GL_UNSIGNED_INT_24_8;
        default:
            throw new RuntimeException("Unexpected format value: " + format);
        }
    }

    /**
     * This shouldn't be used for INT_BIT_FIELD types.
     */
    public static int getGLType(DataType type) {
        switch (type) {
        case FLOAT:
            return GL11.GL_FLOAT;
        case HALF_FLOAT:
            return GL30.GL_HALF_FLOAT;
        case INT:
        case NORMALIZED_INT: // normalization is determined by the use case
            return GL11.GL_INT;
        case UNSIGNED_INT:
        case UNSIGNED_NORMALIZED_INT:
        case INT_BIT_FIELD: // best match here, more specific values are determined by use case
            return GL11.GL_UNSIGNED_INT;
        case SHORT:
        case NORMALIZED_SHORT:
            return GL11.GL_SHORT;
        case UNSIGNED_SHORT:
        case UNSIGNED_NORMALIZED_SHORT:
            return GL11.GL_UNSIGNED_SHORT;
        case BYTE:
        case NORMALIZED_BYTE:
            return GL11.GL_BYTE;
        case UNSIGNED_BYTE:
        case UNSIGNED_NORMALIZED_BYTE:
            return GL11.GL_UNSIGNED_BYTE;
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
            return GL14.GL_FUNC_ADD;
        case MAX:
            return GL14.GL_MAX;
        case MIN:
            return GL14.GL_MIN;
        case REVERSE_SUBTRACT:
            return GL14.GL_FUNC_REVERSE_SUBTRACT;
        case SUBTRACT:
            return GL14.GL_FUNC_SUBTRACT;
        }

        return -1;
    }

    /**
     * Src must not be null.
     */
    public static int getGLBlendFactor(BlendFactor src) {
        switch (src) {
        case ZERO:
            return GL11.GL_ZERO;
        case ONE:
            return GL11.GL_ONE;
        case SRC_COLOR:
            return GL11.GL_SRC_COLOR;
        case ONE_MINUS_SRC_COLOR:
            return GL11.GL_ONE_MINUS_SRC_COLOR;
        case SRC_ALPHA:
            return GL11.GL_SRC_ALPHA;
        case ONE_MINUS_SRC_ALPHA:
            return GL11.GL_ONE_MINUS_SRC_ALPHA;
        case SRC_ALPHA_SATURATE:
            return GL11.GL_SRC_ALPHA_SATURATE;
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
            return GL11.GL_LINE;
        case POINT:
            return GL11.GL_POINT;
        case SOLID:
            return GL11.GL_FILL;
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
            return GL11.GL_DECR;
        case DECREMENT_WRAP:
            return GL14.GL_DECR_WRAP;
        case INCREMENT:
            return GL11.GL_INCR;
        case INCREMENT_WRAP:
            return GL14.GL_INCR_WRAP;
        case ZERO:
            return GL11.GL_ZERO;
        case KEEP:
            return GL11.GL_KEEP;
        case REPLACE:
            return GL11.GL_REPLACE;
        case INVERT:
            return GL11.GL_INVERT;
        }

        return -1;
    }

    /**
     * Should not be called with ATTRIBUTE or null, as nothing parallels its meaning.
     */
    public static int getGLTexGen(TexCoordSource gen) {
        switch (gen) {
        case EYE:
            return GL11.GL_EYE_LINEAR;
        case OBJECT:
            return GL11.GL_OBJECT_LINEAR;
        case SPHERE:
            return GL11.GL_SPHERE_MAP;
        case REFLECTION:
            return GL13.GL_REFLECTION_MAP;
        case NORMAL:
            return GL13.GL_NORMAL_MAP;
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
            return GL11.GL_ADD;
        case ADD_SIGNED:
            return GL13.GL_ADD_SIGNED;
        case DOT3_RGB:
            return GL13.GL_DOT3_RGB;
        case DOT3_RGBA:
            return GL13.GL_DOT3_RGBA;
        case INTERPOLATE:
            return GL13.GL_INTERPOLATE;
        case MODULATE:
            return GL11.GL_MODULATE;
        case REPLACE:
            return GL11.GL_REPLACE;
        case SUBTRACT:
            return GL13.GL_SUBTRACT;
        }

        return -1;
    }

    /**
     * Op must not be null.
     */
    public static int getGLCombineOp(CombineOperand op) {
        switch (op) {
        case ALPHA:
            return GL11.GL_SRC_ALPHA;
        case COLOR:
            return GL11.GL_SRC_COLOR;
        case ONE_MINUS_ALPHA:
            return GL11.GL_ONE_MINUS_SRC_ALPHA;
        case ONE_MINUS_COLOR:
            return GL11.GL_ONE_MINUS_SRC_COLOR;
        }

        return -1;
    }

    /**
     * Src must not be null.
     */
    public static int getGLCombineSrc(CombineSource src) {
        switch (src) {
        case CONST_COLOR:
            return GL13.GL_CONSTANT;
        case CURR_TEX:
            return GL11.GL_TEXTURE;
        case PREV_TEX:
            return GL13.GL_PREVIOUS;
        case VERTEX_COLOR:
            return GL13.GL_PRIMARY_COLOR;
        case TEX0:
            return GL13.GL_TEXTURE0;
        case TEX1:
            return GL13.GL_TEXTURE1;
        case TEX2:
            return GL13.GL_TEXTURE2;
        case TEX3:
            return GL13.GL_TEXTURE3;
        case TEX4:
            return GL13.GL_TEXTURE4;
        case TEX5:
            return GL13.GL_TEXTURE5;
        case TEX6:
            return GL13.GL_TEXTURE6;
        case TEX7:
            return GL13.GL_TEXTURE7;
        case TEX8:
            return GL13.GL_TEXTURE8;
        case TEX9:
            return GL13.GL_TEXTURE9;
        case TEX10:
            return GL13.GL_TEXTURE10;
        case TEX11:
            return GL13.GL_TEXTURE11;
        case TEX12:
            return GL13.GL_TEXTURE12;
        case TEX13:
            return GL13.GL_TEXTURE13;
        case TEX14:
            return GL13.GL_TEXTURE14;
        case TEX15:
            return GL13.GL_TEXTURE15;
        case TEX16:
            return GL13.GL_TEXTURE16;
        case TEX17:
            return GL13.GL_TEXTURE17;
        case TEX18:
            return GL13.GL_TEXTURE18;
        case TEX19:
            return GL13.GL_TEXTURE19;
        case TEX20:
            return GL13.GL_TEXTURE20;
        case TEX21:
            return GL13.GL_TEXTURE21;
        case TEX22:
            return GL13.GL_TEXTURE22;
        case TEX23:
            return GL13.GL_TEXTURE23;
        case TEX24:
            return GL13.GL_TEXTURE24;
        case TEX25:
            return GL13.GL_TEXTURE25;
        case TEX26:
            return GL13.GL_TEXTURE26;
        case TEX27:
            return GL13.GL_TEXTURE27;
        case TEX28:
            return GL13.GL_TEXTURE28;
        case TEX29:
            return GL13.GL_TEXTURE29;
        case TEX30:
            return GL13.GL_TEXTURE30;
        case TEX31:
            return GL13.GL_TEXTURE31;
        }

        return -1;
    }

    /**
     * Utility method to invoke a Runnable on the AWT event dispatch thread (e.g. for modifying AWT and Swing
     * components). This will throw a runtime exception if a problem occurs. It works properly if called from
     * the AWT thread. This should be used when EventQueue.invokeAndWait() or SwingUtilities.invokeAndWait()
     * would be used, except that this is thread safe.
     */
    public static void invokeOnAWTThread(Runnable r, boolean block) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            if (block) {
                try {
                    EventQueue.invokeAndWait(r);
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                EventQueue.invokeLater(r);
            }
        }
    }
}

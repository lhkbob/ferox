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

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.DataType;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.builder.SamplerBuilder;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractSamplerBuilder;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *
 */
public abstract class LwjglSamplerBuilder<T extends Sampler, B extends SamplerBuilder<B>>
        extends AbstractSamplerBuilder<T, B> {
    public LwjglSamplerBuilder(Class<B> builderType, Class<T> textureType, TextureImpl.Target target,
                               FrameworkImpl framework) {
        super(builderType, textureType, target, framework);
    }

    @Override
    protected int generateTextureID(OpenGLContext context) {
        return GL11.glGenTextures();
    }
    // FIXME implement glUnpack state setting

    public static void refreshTexture(OpenGLContext context, Sampler sampler) {
        TextureImpl t = (TextureImpl) sampler;
        TextureImpl.TextureHandle h = t.getHandle();

        int srcFormat = Utils.getGLSrcFormat(t.getFullFormat(), t.getFramework().getCapabilities());
        int type = Utils.getGLType(t.getDataType());
        if (t.getDataType().equals(DataType.INT_BIT_FIELD)) {
            type = Utils.getGLDataTypeForPackedTextureFormat(t.getFullFormat());
        }

        context.bindTexture(0, h);
        switch (h.target) {
        case TEX_1D:
            for (int i = t.getBaseMipmap(); i <= t.getMaxMipmap(); i++) {
                if (t.getDataArray(0, i) != null) {
                    GL11.glTexSubImage1D(GL11.GL_TEXTURE_1D, i, 0, Math.max(t.getWidth() >> i, 1), srcFormat,
                                         type, BufferUtil.newBuffer(t.getDataArray(0, i)));
                }
            }
            break;
        case TEX_2D:
            for (int i = t.getBaseMipmap(); i <= t.getMaxMipmap(); i++) {
                if (t.getDataArray(0, i) != null) {
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, i, 0, 0, Math.max(t.getWidth() >> i, 1),
                                         Math.max(t.getHeight() >> i, 1), srcFormat, type,
                                         BufferUtil.newBuffer(t.getDataArray(0, i)));
                }
            }
            break;
        case TEX_3D:
            for (int i = t.getBaseMipmap(); i <= t.getMaxMipmap(); i++) {
                if (t.getDataArray(0, i) != null) {
                    GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, i, 0, 0, 0, Math.max(t.getWidth() >> i, 1),
                                         Math.max(t.getHeight() >> i, 1), Math.max(t.getDepth() >> i, 1),
                                         srcFormat, type, BufferUtil.newBuffer(t.getDataArray(0, i)));
                }
            }
            break;
        case TEX_CUBEMAP:
            for (int s = 0; s < 6; s++) {
                for (int i = t.getBaseMipmap(); i <= t.getMaxMipmap(); i++) {
                    if (t.getDataArray(s, i) != null) {
                        GL11.glTexSubImage2D(Utils.getGLCubeFace(s), i, 0, 0, Math.max(t.getWidth() >> i, 1),
                                             Math.max(t.getHeight() >> i, 1), srcFormat, type,
                                             BufferUtil.newBuffer(t.getDataArray(s, i)));
                    }
                }
            }
            break;
        case TEX_2D_ARRAY:
            for (int i = 0; i < t.getImageCount(); i++) {
                for (int m = t.getBaseMipmap(); m <= t.getMaxMipmap(); m++) {
                    if (t.getDataArray(i, m) != null) {
                        GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m, 0, 0, i,
                                             Math.max(t.getWidth() >> i, 1), Math.max(t.getHeight() >> i, 1),
                                             1, srcFormat, type, BufferUtil.newBuffer(t.getDataArray(i, m)));
                    }
                }
            }
            break;
        case TEX_1D_ARRAY:
            for (int i = 0; i < t.getImageCount(); i++) {
                for (int m = t.getBaseMipmap(); m <= t.getMaxMipmap(); m++) {
                    if (t.getDataArray(i, m) != null) {
                        GL11.glTexSubImage2D(GL30.GL_TEXTURE_1D_ARRAY, m, 0, i,
                                             Math.max(t.getWidth() >> i, 1), 1, srcFormat, type,
                                             BufferUtil.newBuffer(t.getDataArray(i, m)));
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void pushImage(OpenGLContext context, int image, int mipmap, ByteBuffer imageData,
                             TextureImpl.FullFormat format, int width, int height, int depth) {
        int target = (this.target == TextureImpl.Target.TEX_CUBEMAP ? Utils.getGLCubeFace(image)
                                                                    : Utils.getGLTextureTarget(this.target));
        int srcFormat = Utils.getGLSrcFormat(format, framework.getCapabilities());
        int dstFormat = Utils.getGLDstFormat(format, framework.getCapabilities());

        int type = Utils.getGLType(format.getType());
        if (format.getType().equals(DataType.INT_BIT_FIELD)) {
            type = Utils.getGLDataTypeForPackedTextureFormat(format);
        }

        switch (this.target) {
        case TEX_1D:
            GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, imageData);
            break;
        case TEX_2D:
        case TEX_CUBEMAP:
        case TEX_1D_ARRAY:
            if (srcFormat > 0) {
                // uncompressed (height is automatically set to image length for 1D-arrays
                GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, imageData);
            } else {
                // compressed - always a ByteBuffer
                GL13.glCompressedTexImage2D(target, mipmap, dstFormat, width, height, 0, imageData);
            }
            break;
        case TEX_3D:
        case TEX_2D_ARRAY:
            GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, imageData);
            break;
        default:
            throw new RuntimeException("Unexpected texture target: " + this.target);
        }
    }

    @Override
    protected void setBorderColor(OpenGLContext context, @Const Vector4 borderColor) {
        FloatBuffer color = BufferUtil.newByteBuffer(DataType.FLOAT, 4).asFloatBuffer();
        borderColor.get(color, 0);
        color.rewind();
        GL11.glTexParameter(Utils.getGLTextureTarget(target), GL11.GL_TEXTURE_BORDER_COLOR, color);
    }

    @Override
    protected void setAnisotropy(OpenGLContext context, double anisotropy) {
        float amount = (float) (anisotropy * framework.getCapabilities().getMaxAnisotropicLevel()) + 1f;
        GL11.glTexParameterf(Utils.getGLTextureTarget(target),
                             EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
    }

    @Override
    protected void setWrapMode(OpenGLContext context, Sampler.WrapMode mode) {
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL11.GL_TEXTURE_WRAP_S,
                             Utils.getGLWrapMode(mode));
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL11.GL_TEXTURE_WRAP_T,
                             Utils.getGLWrapMode(mode));
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL12.GL_TEXTURE_WRAP_R,
                             Utils.getGLWrapMode(mode));
    }

    @Override
    protected void setInterpolated(OpenGLContext context, boolean interpolated, boolean hasMipmaps) {
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL11.GL_TEXTURE_MIN_FILTER,
                             Utils.getGLMinFilter(interpolated, hasMipmaps));
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL11.GL_TEXTURE_MAG_FILTER,
                             Utils.getGLMagFilter(interpolated));
    }

    @Override
    protected void setMipmapRange(OpenGLContext context, int base, int max) {
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL12.GL_TEXTURE_BASE_LEVEL, base);
        GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL12.GL_TEXTURE_MAX_LEVEL, max);
    }

    @Override
    protected void setDepthComparison(OpenGLContext context, Renderer.Comparison comparison) {
        if (comparison == null) {
            GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL14.GL_TEXTURE_COMPARE_MODE,
                                 GL11.GL_NONE);
        } else {
            GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL14.GL_TEXTURE_COMPARE_MODE,
                                 GL14.GL_COMPARE_R_TO_TEXTURE);
            GL11.glTexParameteri(Utils.getGLTextureTarget(target), GL14.GL_TEXTURE_COMPARE_FUNC,
                                 Utils.getGLPixelTest(comparison));
        }
    }
}

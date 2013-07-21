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
package com.ferox.renderer.impl.resources;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.DataType;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;


/**
 *
 */
public abstract class TextureImpl extends AbstractResource<TextureImpl.TextureHandle> implements Sampler {
    public static enum FullFormat {
        DEPTH_24BIT(Sampler.TexelFormat.DEPTH, DataType.UNSIGNED_NORMALIZED_INT),
        DEPTH_16BIT(Sampler.TexelFormat.DEPTH, DataType.UNSIGNED_NORMALIZED_SHORT),
        DEPTH_FLOAT(Sampler.TexelFormat.DEPTH, DataType.FLOAT),

        DEPTH_24BIT_STENCIL_8BIT(Sampler.TexelFormat.DEPTH_STENCIL, DataType.INT_BIT_FIELD),

        R_FLOAT(Sampler.TexelFormat.R, DataType.FLOAT),
        R_BYTE(Sampler.TexelFormat.R, DataType.BYTE),
        R_SHORT(Sampler.TexelFormat.R, DataType.SHORT),
        R_INT(Sampler.TexelFormat.R, DataType.INT),
        R_UBYTE(Sampler.TexelFormat.R, DataType.UNSIGNED_BYTE),
        R_USHORT(Sampler.TexelFormat.R, DataType.UNSIGNED_SHORT),
        R_UINT(Sampler.TexelFormat.R, DataType.UNSIGNED_INT),
        R_NORMALIZED_UBYTE(Sampler.TexelFormat.R, DataType.UNSIGNED_NORMALIZED_BYTE),
        R_NORMALIZED_USHORT(Sampler.TexelFormat.R, DataType.UNSIGNED_NORMALIZED_SHORT),
        R_NORMALIZED_UINT(Sampler.TexelFormat.R, DataType.UNSIGNED_NORMALIZED_INT),
        R_HALF_FLOAT(Sampler.TexelFormat.R, DataType.HALF_FLOAT),

        RG_FLOAT(Sampler.TexelFormat.RG, DataType.FLOAT),
        RG_BYTE(Sampler.TexelFormat.RG, DataType.BYTE),
        RG_SHORT(Sampler.TexelFormat.RG, DataType.SHORT),
        RG_INT(Sampler.TexelFormat.RG, DataType.INT),
        RG_UBYTE(Sampler.TexelFormat.RG, DataType.UNSIGNED_BYTE),
        RG_USHORT(Sampler.TexelFormat.RG, DataType.UNSIGNED_SHORT),
        RG_UINT(Sampler.TexelFormat.RG, DataType.UNSIGNED_INT),
        RG_NORMALIZED_UBYTE(Sampler.TexelFormat.RG, DataType.UNSIGNED_NORMALIZED_BYTE),
        RG_NORMALIZED_USHORT(Sampler.TexelFormat.RG, DataType.UNSIGNED_NORMALIZED_SHORT),
        RG_NORMALIZED_UINT(Sampler.TexelFormat.RG, DataType.UNSIGNED_NORMALIZED_INT),
        RG_HALF_FLOAT(Sampler.TexelFormat.RG, DataType.HALF_FLOAT),

        RGB_FLOAT(Sampler.TexelFormat.RGB, DataType.FLOAT),
        RGB_BYTE(Sampler.TexelFormat.RGB, DataType.BYTE),
        RGB_SHORT(Sampler.TexelFormat.RGB, DataType.SHORT),
        RGB_INT(Sampler.TexelFormat.RGB, DataType.INT),
        RGB_UBYTE(Sampler.TexelFormat.RGB, DataType.UNSIGNED_BYTE),
        RGB_USHORT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_SHORT),
        RGB_UINT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_INT),
        RGB_NORMALIZED_UBYTE(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_BYTE),
        RGB_NORMALIZED_USHORT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_SHORT),
        RGB_NORMALIZED_UINT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_INT),
        RGB_HALF_FLOAT(Sampler.TexelFormat.RGB, DataType.HALF_FLOAT),
        RGB_PACKED_FLOAT(Sampler.TexelFormat.RGB, DataType.INT_BIT_FIELD),
        RGB_DXT1(Sampler.TexelFormat.COMPRESSED_RGB, DataType.UNSIGNED_BYTE),

        BGR_FLOAT(Sampler.TexelFormat.RGB, DataType.FLOAT),
        BGR_BYTE(Sampler.TexelFormat.RGB, DataType.BYTE),
        BGR_SHORT(Sampler.TexelFormat.RGB, DataType.SHORT),
        BGR_INT(Sampler.TexelFormat.RGB, DataType.INT),
        BGR_UBYTE(Sampler.TexelFormat.RGB, DataType.UNSIGNED_BYTE),
        BGR_USHORT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_SHORT),
        BGR_UINT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_INT),
        BGR_NORMALIZED_UBYTE(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_BYTE),
        BGR_NORMALIZED_USHORT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_SHORT),
        BGR_NORMALIZED_UINT(Sampler.TexelFormat.RGB, DataType.UNSIGNED_NORMALIZED_INT),
        BGR_HALF_FLOAT(Sampler.TexelFormat.RGB, DataType.HALF_FLOAT),

        RGBA_FLOAT(Sampler.TexelFormat.RGBA, DataType.FLOAT),
        RGBA_BYTE(Sampler.TexelFormat.RGBA, DataType.BYTE),
        RGBA_SHORT(Sampler.TexelFormat.RGBA, DataType.SHORT),
        RGBA_INT(Sampler.TexelFormat.RGBA, DataType.INT),
        RGBA_UBYTE(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_BYTE),
        RGBA_USHORT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_SHORT),
        RGBA_UINT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_INT),
        RGBA_NORMALIZED_UBYTE(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_BYTE),
        RGBA_NORMALIZED_USHORT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_SHORT),
        RGBA_NORMALIZED_UINT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_INT),
        RGBA_HALF_FLOAT(Sampler.TexelFormat.RGBA, DataType.HALF_FLOAT),
        RGBA_DXT1(Sampler.TexelFormat.COMPRESSED_RGBA, DataType.UNSIGNED_BYTE),
        RGBA_DXT3(Sampler.TexelFormat.COMPRESSED_RGBA, DataType.UNSIGNED_BYTE),
        RGBA_DXT5(Sampler.TexelFormat.COMPRESSED_RGBA, DataType.UNSIGNED_BYTE),

        BGRA_FLOAT(Sampler.TexelFormat.RGBA, DataType.FLOAT),
        BGRA_BYTE(Sampler.TexelFormat.RGBA, DataType.BYTE),
        BGRA_SHORT(Sampler.TexelFormat.RGBA, DataType.SHORT),
        BGRA_INT(Sampler.TexelFormat.RGBA, DataType.INT),
        BGRA_UBYTE(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_BYTE),
        BGRA_USHORT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_SHORT),
        BGRA_UINT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_INT),
        BGRA_NORMALIZED_UBYTE(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_BYTE),
        BGRA_NORMALIZED_USHORT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_SHORT),
        BGRA_NORMALIZED_UINT(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_NORMALIZED_INT),
        BGRA_HALF_FLOAT(Sampler.TexelFormat.RGBA, DataType.HALF_FLOAT),

        ARGB_NORMALIZED_UBYTE(Sampler.TexelFormat.RGBA, DataType.UNSIGNED_BYTE),
        ARGB_PACKED_INT(Sampler.TexelFormat.RGBA, DataType.INT_BIT_FIELD);

        private final Sampler.TexelFormat texelFormat;
        private final DataType dataType;

        private FullFormat(Sampler.TexelFormat texelFormat, DataType dataType) {
            this.texelFormat = texelFormat;
            this.dataType = dataType;
        }

        public Sampler.TexelFormat getFormat() {
            return texelFormat;
        }

        public DataType getType() {
            return dataType;
        }
    }

    public static final int POSITIVE_X = 0;
    public static final int NEGATIVE_X = 1;
    public static final int POSITIVE_Y = 2;
    public static final int NEGATIVE_Y = 3;
    public static final int POSITIVE_Z = 4;
    public static final int NEGATIVE_Z = 5;

    private final FullFormat format;
    private final Vector4 borderColor;
    private final double anisotropicFiltering;
    private final Renderer.Comparison depthComparison;
    private final boolean interpolated;
    private final WrapMode wrapMode;

    private final int width;
    private final int height;
    private final int depth;

    private final Object[][] dataArrays;
    private final int baseMipmap;
    private final int maxMipmap;

    public TextureImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                       Vector4 borderColor, double anisotropicFiltering, Renderer.Comparison depthComparison,
                       boolean interpolated, WrapMode wrapMode, Object[][] dataArrays, int baseMipmap,
                       int maxMipmap) {
        super(handle);

        this.format = format;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.anisotropicFiltering = anisotropicFiltering;
        this.borderColor = borderColor;
        this.depthComparison = depthComparison;
        this.interpolated = interpolated;
        this.wrapMode = wrapMode;

        this.dataArrays = dataArrays;
        this.baseMipmap = baseMipmap;
        this.maxMipmap = maxMipmap;
    }

    public Object getDataArray(int image, int mipmap) {
        return dataArrays[image][mipmap];
    }

    public FullFormat getFullFormat() {
        return format;
    }

    public Renderer.Comparison getDepthComparison() {
        return depthComparison;
    }

    public double getBorderDepth() {
        return borderColor.x;
    }

    public double getAnisotropicFiltering() {
        return anisotropicFiltering;
    }

    @Const
    public Vector4 getBorderColor() {
        return borderColor;
    }

    @Override
    public boolean isInterpolated() {
        return interpolated;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public WrapMode getWrapMode() {
        return wrapMode;
    }

    @Override
    public int getBaseMipmap() {
        return baseMipmap;
    }

    @Override
    public int getMaxMipmap() {
        return maxMipmap;
    }

    @Override
    public TexelFormat getFormat() {
        return format.getFormat();
    }

    @Override
    public DataType getDataType() {
        return format.getType();
    }

    public int getImageCount() {
        return dataArrays.length;
    }

    public RenderTarget getRenderTarget(int layer) {
        if (layer < 0) {
            throw new IndexOutOfBoundsException("Target index cannot be negative: " + layer);
        }

        if (getHandle().target == Target.TEX_3D) {
            if (layer >= depth) {
                throw new IndexOutOfBoundsException(
                        "Target index must be less than depth (" + depth + "): " + layer);
            }
        } else {
            if (layer >= dataArrays.length) {
                throw new IndexOutOfBoundsException("Target index must be less than image count (" +
                                                    dataArrays.length + "): " + layer);
            }
        }

        return new RenderTargetImpl(this, layer);
    }

    public RenderTarget getRenderTarget() {
        return getRenderTarget(0);
    }

    public RenderTarget getPositiveXRenderTarget() {
        return getRenderTarget(POSITIVE_X);
    }

    public RenderTarget getNegativeXRenderTarget() {
        return getRenderTarget(NEGATIVE_X);
    }

    public RenderTarget getPositiveYRenderTarget() {
        return getRenderTarget(POSITIVE_Y);
    }

    public RenderTarget getNegativeYRenderTarget() {
        return getRenderTarget(NEGATIVE_Y);
    }

    public RenderTarget getPositiveZRenderTarget() {
        return getRenderTarget(POSITIVE_Z);
    }

    public RenderTarget getNegativeZRenderTarget() {
        return getRenderTarget(NEGATIVE_Z);
    }

    public static class RenderTargetImpl implements RenderTarget {
        public final TextureImpl texture;
        public final int image;

        public RenderTargetImpl(TextureImpl texture, int image) {
            this.texture = texture;
            this.image = image;
        }

        @Override
        public TextureImpl getSampler() {
            return texture;
        }
    }

    /**
     * Target represenst the OpenGL texture targets, which is why the depth samplers are not specified here
     * because in OpenGL they are designated by their format only.
     */
    public static enum Target {
        TEX_1D,
        TEX_2D,
        TEX_3D,
        TEX_CUBEMAP,
        TEX_2D_ARRAY,
        TEX_1D_ARRAY
    }

    public static class TextureHandle extends ResourceHandle {
        public final int texID;
        public final Target target;

        public TextureHandle(FrameworkImpl framework, Target target, int texID) {
            super(framework);
            this.target = target;
            this.texID = texID;
        }

        @Override
        protected void destroyImpl(OpenGLContext context) {
            TextureHandle[] units = context.getState().textures;
            for (int i = 0; i < units.length; i++) {
                if (units[i] == this) {
                    context.bindTexture(i, null);
                }
            }
            getFramework().getResourceFactory().deleteTexture(context, this);
        }
    }
}

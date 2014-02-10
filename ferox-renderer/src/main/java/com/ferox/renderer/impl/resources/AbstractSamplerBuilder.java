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

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.*;
import com.ferox.renderer.builder.ArrayImageData;
import com.ferox.renderer.builder.CubeImageData;
import com.ferox.renderer.builder.DepthMapBuilder.DepthData;
import com.ferox.renderer.builder.DepthMapBuilder.DepthStencilData;
import com.ferox.renderer.builder.ImageData;
import com.ferox.renderer.builder.SamplerBuilder;
import com.ferox.renderer.builder.TextureBuilder.ARGBData;
import com.ferox.renderer.builder.TextureBuilder.BasicColorData;
import com.ferox.renderer.builder.TextureBuilder.CompressedRGBAData;
import com.ferox.renderer.builder.TextureBuilder.CompressedRGBData;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.nio.ByteBuffer;


/**
 *
 */
public abstract class AbstractSamplerBuilder<T extends Sampler, B extends SamplerBuilder<B>>
        extends AbstractBuilder<T, TextureImpl.TextureHandle> {
    private int width;
    private int height;
    private int depth;
    private int imageCount;

    private double anisotropy;
    private final Vector4 borderColor;
    private boolean interpolated;
    private Sampler.WrapMode wrapMode;

    private Renderer.Comparison depthComparison;

    private Object[][] imageData;
    private TextureImpl.FullFormat[][] imageFormats;

    private final Class<B> builderType;
    protected final Class<T> textureType;
    protected final TextureImpl.Target target;

    // cached in validate()
    private TextureImpl.FullFormat detectedFormat;
    private int detectedBaseMipmap;
    private int detectedMaxMipmap;

    public AbstractSamplerBuilder(Class<B> builderType, Class<T> textureType, TextureImpl.Target target,
                                  FrameworkImpl framework) {
        super(framework);
        this.builderType = builderType;
        this.textureType = textureType;
        this.target = target;
        borderColor = new Vector4();
        wrapMode = Sampler.WrapMode.CLAMP;
    }

    public B anisotropy(double v) {
        anisotropy = v;
        return builderType.cast(this);
    }

    public B borderColor(@Const ColorRGB color) {
        borderColor.set(color.redHDR(), color.greenHDR(), color.blueHDR(), 1.0);
        return builderType.cast(this);
    }

    public B borderColor(@Const Vector4 color) {
        borderColor.set(color);
        return builderType.cast(this);
    }

    public B interpolated() {
        interpolated = true;
        return builderType.cast(this);
    }

    public B wrap(Sampler.WrapMode wrap) {
        if (wrap == null) {
            throw new NullPointerException("WrapMode cannot be null");
        }
        wrapMode = wrap;
        return builderType.cast(this);
    }

    public B width(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be at least 1: " + width);
        }
        this.width = width;
        return builderType.cast(this);
    }

    public B height(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be at least 1: " + width);
        }
        this.height = height;
        return builderType.cast(this);
    }

    public B length(int side) {
        width(side);
        return builderType.cast(this);
    }

    public B side(int side) {
        width(side);
        height(side);
        return builderType.cast(this);
    }

    public B depth(int depth) {
        if (depth <= 0) {
            throw new IllegalArgumentException("Depth must be at least 1: " + width);
        }
        this.depth = depth;
        return builderType.cast(this);
    }

    public B imageCount(int imageCount) {
        if (imageCount <= 0) {
            throw new IllegalArgumentException("All samplers must have at least one image: " + imageCount);
        }
        this.imageCount = imageCount;
        return builderType.cast(this);
    }

    public B depthComparison(Renderer.Comparison depthComparison) {
        this.depthComparison = depthComparison;
        return builderType.cast(this);
    }

    public B borderDepth(double depth) {
        borderColor.set(depth, 0, 0, 1);
        return builderType.cast(this);
    }

    private void allocateImages() {
        // some additional validation is done here that cannot be changed
        // after the image builder is received
        if (width <= 0) {
            throw new ResourceException("Width must be specified");
        }
        if (height <= 0) {
            throw new ResourceException("Height must be specified");
        }
        if (depth <= 0) {
            throw new ResourceException("Depth must be specified");
        }
        if (imageCount <= 0) {
            throw new ResourceException("Image count must be specified");
        }

        int max = Math.max(width, Math.max(height, depth));
        int mipmaps = (int) Math.floor(Math.log(max) / Math.log(2)) + 1;

        imageData = new Object[imageCount][mipmaps];
        imageFormats = new TextureImpl.FullFormat[imageCount][mipmaps];
    }

    private int getBufferSize(int mipmap) {
        int w = Math.max(width >> mipmap, 1);
        int h = Math.max(height >> mipmap, 1);
        int d = Math.max(depth >> mipmap, 1);

        switch (detectedFormat) {
        case ARGB_PACKED_INT:
        case RGB_PACKED_FLOAT:
        case DEPTH_24BIT_STENCIL_8BIT:
            // these formats are packed so that a single primitive holds
            // the complete pixel data regardless of component count
            return w * h * d;
        case RGB_DXT1:
        case RGBA_DXT1:
            // dxt1 uses a special equation (and it's only used in 2D images
            return (int) (8 * Math.ceil(w / 4.0) * Math.ceil(h / 4.0));
        case RGBA_DXT3:
        case RGBA_DXT5:
            // dxt3 + 5 compression has a higher block size
            return (int) (16 * Math.ceil(w / 4.0) * Math.ceil(h / 4.0));
        default:
            // there is a primitive for each component in a pixel
            return detectedFormat.getFormat().getComponentCount() * w * h * d;
        }
    }

    private ByteBuffer consolidateBuffer(int mipmap) {
        int perImage = getBufferSize(mipmap) * detectedFormat.getType().getByteCount();
        int size = perImage * imageCount;

        ByteBuffer b = BufferUtil.newByteBuffer(detectedFormat.getType(), size);

        Class<?> primitive = detectedFormat.getType().getJavaPrimitive();
        if (float.class.equals(primitive)) {
            for (int i = 0; i < imageCount; i++) {
                if (imageData[i][mipmap] != null) {
                    b.position(i * perImage).limit((i + 1) * perImage);
                    b.asFloatBuffer().put((float[]) imageData[i][mipmap]);
                }
            }
        } else if (int.class.equals(primitive)) {
            for (int i = 0; i < imageCount; i++) {
                if (imageData[i][mipmap] != null) {
                    b.position(i * perImage).limit((i + 1) * perImage);
                    b.asIntBuffer().put((int[]) imageData[i][mipmap]);
                }
            }
        } else if (short.class.equals(primitive)) {
            for (int i = 0; i < imageCount; i++) {
                if (imageData[i][mipmap] != null) {
                    b.position(i * perImage).limit((i + 1) * perImage);
                    b.asShortBuffer().put((short[]) imageData[i][mipmap]);
                }
            }
        } else { // byte
            for (int i = 0; i < imageCount; i++) {
                if (imageData[i][mipmap] != null) {
                    b.position(i * perImage).limit((i + 1) * perImage);
                    b.put((byte[]) imageData[i][mipmap]);
                }
            }
        }
        b.clear();
        return b;
    }

    @Override
    protected void validate() {
        // verify texture target support
        if (!framework.getCapabilities().getSupportedTextureTargets().contains(textureType)) {
            throw new ResourceException(String.format("%s textures are not supported on current hardware",
                                                      textureType));
        }
        if (imageFormats == null) {
            throw new ResourceException("No texel format selected");
        }

        // detect and validate image format consistency
        for (int i = 0; i < imageFormats.length; i++) {
            for (int j = 0; j < imageFormats[i].length; j++) {
                if (imageFormats[i][j] != null) {
                    if (detectedFormat != null && detectedFormat != imageFormats[i][j]) {
                        throw new ResourceException("Inconsistent data specification, every image and mipmap must use same format and type");
                    }
                    detectedFormat = imageFormats[i][j];
                }
            }
        }
        if (detectedFormat == null) {
            throw new ResourceException("Must specify image format for at least one level");
        }

        // verify format support
        switch (detectedFormat.getFormat()) {
        case DEPTH_STENCIL:
            if (!framework.getCapabilities().getDepthStencilTextureSupport()) {
                throw new ResourceException("Depth+stencil textures aren't supported");
            }
            break;
        case COMPRESSED_RGB:
        case COMPRESSED_RGBA:
            if (!framework.getCapabilities().getS3TextureCompression()) {
                throw new ResourceException("DXT texture compression isn't supported");
            }
            break;
        }

        if (detectedFormat.getType() != DataType.INT_BIT_FIELD &&
            !detectedFormat.getType().isDecimalNumber()) {
            // check for integer texture support
            if (!framework.getCapabilities().getIntegerTextureSupport()) {
                throw new ResourceException("Unnormalized signed and unsigned integer textures are not supported");
            }
        }

        // detect base and max mipmap ranges (using format and not image array since
        // the array can be null for RTT textures)
        detectedBaseMipmap = Integer.MAX_VALUE;
        detectedMaxMipmap = Integer.MIN_VALUE;
        for (int i = 0; i < imageFormats.length; i++) {
            for (int j = 0; j < imageFormats[i].length; j++) {
                if (imageFormats[i][j] != null) {
                    detectedBaseMipmap = Math.min(j, detectedBaseMipmap);
                    break;
                }
            }
            for (int j = imageFormats[i].length - 1; j >= 0; j--) {
                if (imageFormats[i][j] != null) {
                    detectedMaxMipmap = Math.max(j, detectedMaxMipmap);
                    break;
                }
            }
        }

        // validate dimension requirements if it's a compressed texture
        if (detectedFormat.getFormat().isCompressed()) {
            if (width % 4 != 0 || height % 4 != 0) {
                throw new ResourceException("DXT compressed textures must have dimensions that are multiples of 4");
            }
        }

        // verify maximum dimensions are respected
        // FIXME interpretation of max value might assume single byte components, I need to look into that more
        int maxSize = 0;
        switch (target) {
        case TEX_1D:
        case TEX_2D:
        case TEX_1D_ARRAY:
        case TEX_2D_ARRAY:
            maxSize = framework.getCapabilities().getMaxTextureSize();
            break;
        case TEX_CUBEMAP:
            maxSize = framework.getCapabilities().getMaxTextureCubeMapSize();
            break;
        case TEX_3D:
            maxSize = framework.getCapabilities().getMaxTexture3DSize();
            break;
        }

        if (width > maxSize || height > maxSize || depth > maxSize) {
            throw new ResourceException("Dimensions exceed supported maximum of " + maxSize);
        }

        if (target == TextureImpl.Target.TEX_1D_ARRAY || target == TextureImpl.Target.TEX_2D_ARRAY) {
            if (imageCount > framework.getCapabilities().getMaxTextureArrayImages()) {
                throw new ResourceException("Texture array has too many images for hardware");
            }
        }


        // verify array lengths for every image specified
        for (int i = 0; i < imageData.length; i++) {
            for (int j = 0; j < imageData[i].length; j++) {
                int expectedSize = getBufferSize(j);

                if (imageData[i][j] != null) {
                    int actualSize = BufferUtil.getArrayLength(imageData[i][j]);
                    if (actualSize != expectedSize) {
                        throw new ResourceException(String.format("Expected %d elements but got %d (mipmap: %d, image: %d)",
                                                                  expectedSize, actualSize, j, i));
                    }
                }
            }
        }
    }

    @Override
    protected TextureImpl.TextureHandle allocate(OpenGLContext ctx) {
        return new TextureImpl.TextureHandle(framework, target, generateTextureID(ctx));
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, TextureImpl.TextureHandle handle) {
        ctx.bindTexture(0, handle);

        // push data to the GPU based on image format, passing in a null buffer
        // when no data array was given to us
        if (target == TextureImpl.Target.TEX_1D_ARRAY) {
            // must recombine all 1D images into a single buffer and pretend like we're
            // calling glTexture2D
            for (int j = detectedBaseMipmap; j <= detectedMaxMipmap; j++) {
                pushImage(ctx, 0, j, consolidateBuffer(j), detectedFormat, Math.max(width >> j, 1),
                          imageCount, 1);
            }
        } else if (target == TextureImpl.Target.TEX_2D_ARRAY) {
            // must recombine all 2D images into a single buffer and pretend like we're
            // calling glTexture3D
            for (int j = detectedBaseMipmap; j <= detectedMaxMipmap; j++) {
                pushImage(ctx, 0, j, consolidateBuffer(j), detectedFormat, Math.max(width >> j, 1),
                          Math.max(height >> j, 1), imageCount);
            }
        } else {
            // pass in everything in the block that's already been allocated
            // for 1D, 2D, 3D there's only one image, and for cube maps it's 6 and
            // will gracefully look like a call for glTexture2D
            for (int i = 0; i < imageFormats.length; i++) {
                for (int j = 0; j < imageFormats[i].length; j++) {
                    if (imageFormats[i][j] != null) {
                        // allocate a single image
                        int w = Math.max(width >> j, 1);
                        int h = Math.max(height >> j, 1);
                        int d = Math.max(depth >> j, 1);

                        if (imageData[i][j] == null) {
                            pushImage(ctx, i, j, null, detectedFormat, w, h, d);
                        } else {
                            // wrap in a buffer
                            pushImage(ctx, i, j, BufferUtil.newBuffer(imageData[i][j]), detectedFormat, w, h,
                                      d);
                        }
                    }
                }
            }
        }

        setBorderColor(ctx, borderColor);
        setAnisotropy(ctx, anisotropy);
        setWrapMode(ctx, wrapMode);
        setInterpolated(ctx, interpolated, detectedMaxMipmap - detectedBaseMipmap > 1);
        setMipmapRange(ctx, detectedBaseMipmap, detectedMaxMipmap);
        setDepthComparison(ctx, depthComparison);
    }

    protected abstract int generateTextureID(OpenGLContext context);

    protected abstract void pushImage(OpenGLContext context, int image, int mipmap, ByteBuffer imageData,
                                      TextureImpl.FullFormat format, int width, int height, int depth);

    protected abstract void setBorderColor(OpenGLContext context, @Const Vector4 borderColor);

    protected abstract void setAnisotropy(OpenGLContext context, double anisotropy);

    protected abstract void setWrapMode(OpenGLContext context, Sampler.WrapMode mode);

    protected abstract void setInterpolated(OpenGLContext context, boolean interpolated, boolean hasMipmaps);

    protected abstract void setMipmapRange(OpenGLContext context, int base, int max);

    protected abstract void setDepthComparison(OpenGLContext context, Renderer.Comparison comparison);

    protected ImageData<? extends BasicColorData> singleR() {
        allocateImages();
        return new ImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forR(this, 0, level);
            }
        };
    }

    protected ImageData<? extends BasicColorData> singleRG() {
        allocateImages();
        return new ImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forRG(this, 0, level);
            }
        };
    }

    protected ImageData<? extends CompressedRGBData> singleRGB() {
        allocateImages();
        return new ImageDataImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int level) {
                return TextureDatas.forRGB(this, 0, level);
            }
        };
    }

    protected ImageData<? extends BasicColorData> singleBGR() {
        allocateImages();
        return new ImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forBGR(this, 0, level);
            }
        };
    }

    protected ImageData<? extends CompressedRGBAData> singleRGBA() {
        allocateImages();
        return new ImageDataImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int level) {
                return TextureDatas.forRGBA(this, 0, level);
            }
        };
    }

    protected ImageData<? extends BasicColorData> singleBGRA() {
        allocateImages();
        return new ImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forBGRA(this, 0, level);
            }
        };
    }

    protected ImageData<? extends ARGBData> singleARGB() {
        allocateImages();
        return new ImageDataImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int level) {
                return TextureDatas.forARGB(this, 0, level);
            }
        };
    }

    protected ImageData<? extends DepthData> singleDepth() {
        allocateImages();
        return new ImageDataImpl<DepthData>() {
            @Override
            public DepthData mipmap(int level) {
                return TextureDatas.forDepth(this, 0, level);
            }
        };
    }

    protected ImageData<? extends DepthStencilData> singleDepthStencil() {
        allocateImages();
        return new ImageDataImpl<DepthStencilData>() {
            @Override
            public DepthStencilData mipmap(int level) {
                return TextureDatas.forDepthStencil(this, 0, level);
            }
        };
    }

    protected CubeImageData<? extends BasicColorData> cubeR() {
        allocateImages();
        return new CubeImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forR(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends BasicColorData> cubeRG() {
        allocateImages();
        return new CubeImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forRG(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends CompressedRGBData> cubeRGB() {
        allocateImages();
        return new CubeImageDataImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int image, int level) {
                return TextureDatas.forRGB(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends BasicColorData> cubeBGR() {
        allocateImages();
        return new CubeImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGR(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends CompressedRGBAData> cubeRGBA() {
        allocateImages();
        return new CubeImageDataImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int image, int level) {
                return TextureDatas.forRGBA(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends BasicColorData> cubeBGRA() {
        allocateImages();
        return new CubeImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGRA(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends ARGBData> cubeARGB() {
        allocateImages();
        return new CubeImageDataImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int image, int level) {
                return TextureDatas.forARGB(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends DepthData> cubeDepth() {
        allocateImages();
        return new CubeImageDataImpl<DepthData>() {
            @Override
            public DepthData mipmap(int image, int level) {
                return TextureDatas.forDepth(this, image, level);
            }
        };
    }

    protected CubeImageData<? extends DepthStencilData> cubeDepthStencil() {
        allocateImages();
        return new CubeImageDataImpl<DepthStencilData>() {
            @Override
            public DepthStencilData mipmap(int image, int level) {
                return TextureDatas.forDepthStencil(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends BasicColorData> arrayR() {
        allocateImages();
        return new ArrayImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forR(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends BasicColorData> arrayRG() {
        allocateImages();
        return new ArrayImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forRG(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends CompressedRGBData> arrayRGB() {
        allocateImages();
        return new ArrayImageDataImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int image, int level) {
                return TextureDatas.forRGB(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends BasicColorData> arrayBGR() {
        allocateImages();
        return new ArrayImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGR(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends CompressedRGBAData> arrayRGBA() {
        allocateImages();
        return new ArrayImageDataImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int image, int level) {
                return TextureDatas.forRGBA(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends BasicColorData> arrayBGRA() {
        allocateImages();
        return new ArrayImageDataImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGRA(this, image, level);
            }
        };
    }

    protected ArrayImageData<? extends ARGBData> arrayARGB() {
        allocateImages();
        return new ArrayImageDataImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int image, int level) {
                return TextureDatas.forARGB(this, image, level);
            }
        };
    }

    protected Texture1D wrapAsTexture1D(TextureImpl.TextureHandle handle) {
        return new Texture1DImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                 depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                 detectedMaxMipmap);
    }

    protected Texture2D wrapAsTexture2D(TextureImpl.TextureHandle handle) {
        return new Texture2DImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                 depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                 detectedMaxMipmap);
    }

    protected Texture3D wrapAsTexture3D(TextureImpl.TextureHandle handle) {
        return new Texture3DImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                 depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                 detectedMaxMipmap);
    }

    protected TextureCubeMap wrapAsTextureCubeMap(TextureImpl.TextureHandle handle) {
        return new TextureCubeMapImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                      depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                      detectedMaxMipmap);
    }

    protected Texture1DArray wrapAsTexture1DArray(TextureImpl.TextureHandle handle) {
        return new Texture1DArrayImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                      depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                      detectedMaxMipmap);
    }

    protected Texture2DArray wrapAsTexture2DArray(TextureImpl.TextureHandle handle) {
        return new Texture2DArrayImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                      depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                      detectedMaxMipmap);
    }

    protected DepthMap2D wrapAsDepthMap2D(TextureImpl.TextureHandle handle) {
        return new DepthMap2DImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                  depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                  detectedMaxMipmap);
    }

    protected DepthCubeMap wrapAsDepthCubeMap(TextureImpl.TextureHandle handle) {
        return new DepthCubeMapImpl(handle, detectedFormat, width, height, depth, borderColor, anisotropy,
                                    depthComparison, interpolated, wrapMode, imageData, detectedBaseMipmap,
                                    detectedMaxMipmap);
    }

    private abstract class ImageDataImpl<M> implements ImageData<M>, TextureDatas.ImageSpecifier {
        @Override
        public void setImageData(int image, int mipmap, Object array, TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private abstract class CubeImageDataImpl<M> implements CubeImageData<M>, TextureDatas.ImageSpecifier {
        @Override
        public M positiveX(int mipmap) {
            return mipmap(TextureImpl.POSITIVE_X, mipmap);
        }

        @Override
        public M positiveY(int mipmap) {
            return mipmap(TextureImpl.POSITIVE_Y, mipmap);
        }

        @Override
        public M positiveZ(int mipmap) {
            return mipmap(TextureImpl.POSITIVE_Z, mipmap);
        }

        @Override
        public M negativeX(int mipmap) {
            return mipmap(TextureImpl.NEGATIVE_X, mipmap);
        }

        @Override
        public M negativeY(int mipmap) {
            return mipmap(TextureImpl.NEGATIVE_Y, mipmap);
        }

        @Override
        public M negativeZ(int mipmap) {
            return mipmap(TextureImpl.NEGATIVE_Z, mipmap);
        }

        protected abstract M mipmap(int image, int level);

        @Override
        public void setImageData(int image, int mipmap, Object array, TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private abstract class ArrayImageDataImpl<M> implements ArrayImageData<M>, TextureDatas.ImageSpecifier {
        @Override
        public void setImageData(int image, int mipmap, Object array, TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private static class Texture1DImpl extends TextureImpl implements Texture1D {
        public Texture1DImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                             Vector4 borderColor, double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated, WrapMode wrapMode,
                             Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class Texture2DImpl extends TextureImpl implements Texture2D {
        public Texture2DImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                             Vector4 borderColor, double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated, WrapMode wrapMode,
                             Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class Texture3DImpl extends TextureImpl implements Texture3D {
        public Texture3DImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                             Vector4 borderColor, double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated, WrapMode wrapMode,
                             Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class TextureCubeMapImpl extends TextureImpl implements TextureCubeMap {
        public TextureCubeMapImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                                  Vector4 borderColor, double anisotropicFiltering,
                                  Renderer.Comparison depthComparison, boolean interpolated,
                                  WrapMode wrapMode, Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class Texture1DArrayImpl extends TextureImpl implements Texture1DArray {
        public Texture1DArrayImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                                  Vector4 borderColor, double anisotropicFiltering,
                                  Renderer.Comparison depthComparison, boolean interpolated,
                                  WrapMode wrapMode, Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class Texture2DArrayImpl extends TextureImpl implements Texture2DArray {
        public Texture2DArrayImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                                  Vector4 borderColor, double anisotropicFiltering,
                                  Renderer.Comparison depthComparison, boolean interpolated,
                                  WrapMode wrapMode, Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class DepthMap2DImpl extends TextureImpl implements DepthMap2D {
        public DepthMap2DImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                              Vector4 borderColor, double anisotropicFiltering,
                              Renderer.Comparison depthComparison, boolean interpolated, WrapMode wrapMode,
                              Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }

    private static class DepthCubeMapImpl extends TextureImpl implements DepthCubeMap {
        public DepthCubeMapImpl(TextureHandle handle, FullFormat format, int width, int height, int depth,
                                Vector4 borderColor, double anisotropicFiltering,
                                Renderer.Comparison depthComparison, boolean interpolated, WrapMode wrapMode,
                                Object[][] dataArrays, int baseMipmap, int maxMipmap) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering, depthComparison,
                  interpolated, wrapMode, dataArrays, baseMipmap, maxMipmap);
        }
    }
}

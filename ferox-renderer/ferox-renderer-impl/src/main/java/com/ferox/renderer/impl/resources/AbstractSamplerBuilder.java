package com.ferox.renderer.impl.resources;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.*;
import com.ferox.renderer.builder.ArrayImageBuilder;
import com.ferox.renderer.builder.CubeImageBuilder;
import com.ferox.renderer.builder.DepthMapBuilder.DepthData;
import com.ferox.renderer.builder.DepthMapBuilder.DepthStencilData;
import com.ferox.renderer.builder.SamplerBuilder;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.TextureBuilder.ARGBData;
import com.ferox.renderer.builder.TextureBuilder.BasicColorData;
import com.ferox.renderer.builder.TextureBuilder.CompressedRGBAData;
import com.ferox.renderer.builder.TextureBuilder.CompressedRGBData;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

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

    // cached in validate from imageFormats
    private TextureImpl.FullFormat detectedFormat;

    public AbstractSamplerBuilder(Class<B> builderType, FrameworkImpl framework) {
        super(framework);
        this.builderType = builderType;
        borderColor = new Vector4();
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
        wrapMode = wrap;
        return builderType.cast(this);
    }

    public B width(int width) {
        this.width = width;
        return builderType.cast(this);
    }

    public B height(int height) {
        this.height = height;
        return builderType.cast(this);
    }

    public B side(int side) {
        width(side);
        height(side);
        return builderType.cast(this);
    }

    public B depth(int depth) {
        this.depth = depth;
        return builderType.cast(this);
    }

    public B imageCount(int imageCount) {
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

    protected SingleImageBuilder<T, BasicColorData> singleR() {
        allocateImages();
        return new SingleImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forR(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, BasicColorData> singleRG() {
        allocateImages();
        return new SingleImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forRG(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, CompressedRGBData> singleRGB() {
        allocateImages();
        return new SingleImageBuilderImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int level) {
                return TextureDatas.forRGB(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, BasicColorData> singleBGR() {
        allocateImages();
        return new SingleImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forBGR(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, CompressedRGBAData> singleRGBA() {
        allocateImages();
        return new SingleImageBuilderImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int level) {
                return TextureDatas.forRGBA(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, BasicColorData> singleBGRA() {
        allocateImages();
        return new SingleImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int level) {
                return TextureDatas.forBGRA(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, ARGBData> singleARGB() {
        allocateImages();
        return new SingleImageBuilderImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int level) {
                return TextureDatas.forARGB(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, DepthData> singleDepth() {
        allocateImages();
        return new SingleImageBuilderImpl<DepthData>() {
            @Override
            public DepthData mipmap(int level) {
                return TextureDatas.forDepth(this, 0, level);
            }
        };
    }

    protected SingleImageBuilder<T, DepthStencilData> singleDepthStencil() {
        allocateImages();
        return new SingleImageBuilderImpl<DepthStencilData>() {
            @Override
            public DepthStencilData mipmap(int level) {
                return TextureDatas.forDepthStencil(this, 0, level);
            }
        };
    }

    protected CubeImageBuilder<T, BasicColorData> cubeR() {
        allocateImages();
        return new CubeImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forR(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, BasicColorData> cubeRG() {
        allocateImages();
        return new CubeImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forRG(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, CompressedRGBData> cubeRGB() {
        allocateImages();
        return new CubeImageBuilderImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int image, int level) {
                return TextureDatas.forRGB(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, BasicColorData> cubeBGR() {
        allocateImages();
        return new CubeImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGR(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, CompressedRGBAData> cubeRGBA() {
        allocateImages();
        return new CubeImageBuilderImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int image, int level) {
                return TextureDatas.forRGBA(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, BasicColorData> cubeBGRA() {
        allocateImages();
        return new CubeImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGRA(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, ARGBData> cubeARGB() {
        allocateImages();
        return new CubeImageBuilderImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int image, int level) {
                return TextureDatas.forARGB(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, DepthData> cubeDepth() {
        allocateImages();
        return new CubeImageBuilderImpl<DepthData>() {
            @Override
            public DepthData mipmap(int image, int level) {
                return TextureDatas.forDepth(this, image, level);
            }
        };
    }

    protected CubeImageBuilder<T, DepthStencilData> cubeDepthStencil() {
        allocateImages();
        return new CubeImageBuilderImpl<DepthStencilData>() {
            @Override
            public DepthStencilData mipmap(int image, int level) {
                return TextureDatas.forDepthStencil(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, BasicColorData> arrayR() {
        allocateImages();
        return new ArrayImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forR(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, BasicColorData> arrayRG() {
        allocateImages();
        return new ArrayImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forRG(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, CompressedRGBData> arrayRGB() {
        allocateImages();
        return new ArrayImageBuilderImpl<CompressedRGBData>() {
            @Override
            public CompressedRGBData mipmap(int image, int level) {
                return TextureDatas.forRGB(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, BasicColorData> arrayBGR() {
        allocateImages();
        return new ArrayImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGR(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, CompressedRGBAData> arrayRGBA() {
        allocateImages();
        return new ArrayImageBuilderImpl<CompressedRGBAData>() {
            @Override
            public CompressedRGBAData mipmap(int image, int level) {
                return TextureDatas.forRGBA(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, BasicColorData> arrayBGRA() {
        allocateImages();
        return new ArrayImageBuilderImpl<BasicColorData>() {
            @Override
            public BasicColorData mipmap(int image, int level) {
                return TextureDatas.forBGRA(this, image, level);
            }
        };
    }

    protected ArrayImageBuilder<T, ARGBData> arrayARGB() {
        allocateImages();
        return new ArrayImageBuilderImpl<ARGBData>() {
            @Override
            public ARGBData mipmap(int image, int level) {
                return TextureDatas.forARGB(this, image, level);
            }
        };
    }

    private void allocateImages() {
        if (imageData == null) {
            throw new IllegalStateException("Already selected format");
        }
        // FIXME verify equation's correctness
        int mipmaps = Math.max((int) (Math.log(width) + 1),
                               Math.max((int) (Math.log(depth) + 1),
                                        (int) (Math.log(height) + 1)));
        imageData = new Object[imageCount][mipmaps];
    }

    @Override
    protected void validate() {
        // FIXME
    }

    @Override
    protected TextureImpl.TextureHandle allocate(OpenGLContext ctx) {
        return new TextureImpl.TextureHandle(framework, TextureImpl.Target.TEX_2D,
                                             generateTextureID(ctx));
    }

    @Override
    protected void pushToGPU(OpenGLContext ctx, TextureImpl.TextureHandle handle) {
        ctx.bindTexture(0, handle);
        for (int image = 0; image < imageData.length; image++) {
            for (int mipmap = 0; mipmap < imageData[image].length; mipmap++) {
                if (imageData[image][mipmap] != null) {
                    pushImage(ctx, image, mipmap, imageData[image][mipmap],
                              imageFormats[image][mipmap], width >> mipmap,
                              height >> mipmap);
                }
            }
        }

        int baseMipmap = 0;
        int maxMipmap = 0;

        setBorderColor(ctx, borderColor);
        setAnisotropy(ctx, anisotropy);
        setWrapMode(ctx, wrapMode);
        setInterpolated(ctx, interpolated, maxMipmap - baseMipmap > 1);
        setMipmapRange(ctx, baseMipmap, maxMipmap);
        setDepthComparison(ctx, depthComparison);

        // FIXME compute mipmap range and configure that, we should move the computation from
        //  TextureImage() to here since we need it here first
    }

    protected Texture1D wrapAsTexture1D(TextureImpl.TextureHandle handle) {
        return new Texture1DImpl(handle, detectedFormat, width, height, depth,
                                 borderColor, anisotropy, depthComparison, interpolated,
                                 wrapMode, imageData);
    }

    protected Texture2D wrapAsTexture2D(TextureImpl.TextureHandle handle) {
        return new Texture2DImpl(handle, detectedFormat, width, height, depth,
                                 borderColor, anisotropy, depthComparison, interpolated,
                                 wrapMode, imageData);
    }

    protected Texture3D wrapAsTexture3D(TextureImpl.TextureHandle handle) {
        return new Texture3DImpl(handle, detectedFormat, width, height, depth,
                                 borderColor, anisotropy, depthComparison, interpolated,
                                 wrapMode, imageData);
    }

    protected TextureCubeMap wrapAsTextureCubeMap(TextureImpl.TextureHandle handle) {
        return new TextureCubeMapImpl(handle, detectedFormat, width, height, depth,
                                      borderColor, anisotropy, depthComparison,
                                      interpolated, wrapMode, imageData);
    }

    protected Texture1DArray wrapAsTexture1DArray(TextureImpl.TextureHandle handle) {
        return new Texture1DArrayImpl(handle, detectedFormat, width, height, depth,
                                      borderColor, anisotropy, depthComparison,
                                      interpolated, wrapMode, imageData);
    }

    protected Texture2DArray wrapAsTexture2DArray(TextureImpl.TextureHandle handle) {
        return new Texture2DArrayImpl(handle, detectedFormat, width, height, depth,
                                      borderColor, anisotropy, depthComparison,
                                      interpolated, wrapMode, imageData);
    }

    protected DepthMap2D wrapAsDepthMap2D(TextureImpl.TextureHandle handle) {
        return new DepthMap2DImpl(handle, detectedFormat, width, height, depth,
                                  borderColor, anisotropy, depthComparison, interpolated,
                                  wrapMode, imageData);
    }

    protected DepthCubeMap wrapAsDepthCubeMap(TextureImpl.TextureHandle handle) {
        return new DepthCubeMapImpl(handle, detectedFormat, width, height, depth,
                                    borderColor, anisotropy, depthComparison,
                                    interpolated, wrapMode, imageData);
    }

    protected abstract int generateTextureID(OpenGLContext context);

    protected abstract void pushImage(OpenGLContext context, int image, int mipmap,
                                      Object array, TextureImpl.FullFormat format,
                                      int width, int height);

    protected abstract void setBorderColor(OpenGLContext context,
                                           @Const Vector4 borderColor);

    protected abstract void setAnisotropy(OpenGLContext context, double anisotropy);

    protected abstract void setWrapMode(OpenGLContext context, Sampler.WrapMode mode);

    protected abstract void setInterpolated(OpenGLContext context, boolean interpolated,
                                            boolean hasMipmaps);

    protected abstract void setMipmapRange(OpenGLContext context, int base, int max);

    protected abstract void setDepthComparison(OpenGLContext context,
                                               Renderer.Comparison comparison);

    private abstract class SingleImageBuilderImpl<M>
            implements SingleImageBuilder<T, M>, TextureDatas.ImageSpecifier {
        @Override
        public T build() {
            return AbstractSamplerBuilder.this.build();
        }

        @Override
        public void setImageData(int image, int mipmap, Object array,
                                 TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private abstract class CubeImageBuilderImpl<M>
            implements CubeImageBuilder<T, M>, TextureDatas.ImageSpecifier {
        @Override
        public T build() {
            return AbstractSamplerBuilder.this.build();
        }

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
        public void setImageData(int image, int mipmap, Object array,
                                 TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private abstract class ArrayImageBuilderImpl<M>
            implements ArrayImageBuilder<T, M>, TextureDatas.ImageSpecifier {
        @Override
        public T build() {
            return AbstractSamplerBuilder.this.build();
        }

        @Override
        public void setImageData(int image, int mipmap, Object array,
                                 TextureImpl.FullFormat format) {
            imageData[image][mipmap] = array;
            imageFormats[image][mipmap] = format;
        }
    }

    private static class Texture1DImpl extends TextureImpl implements Texture1D {
        public Texture1DImpl(TextureHandle handle, FullFormat format, int width,
                             int height, int depth, Vector4 borderColor,
                             double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated,
                             WrapMode wrapMode, Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class Texture2DImpl extends TextureImpl implements Texture2D {
        public Texture2DImpl(TextureHandle handle, FullFormat format, int width,
                             int height, int depth, Vector4 borderColor,
                             double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated,
                             WrapMode wrapMode, Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class Texture3DImpl extends TextureImpl implements Texture3D {
        public Texture3DImpl(TextureHandle handle, FullFormat format, int width,
                             int height, int depth, Vector4 borderColor,
                             double anisotropicFiltering,
                             Renderer.Comparison depthComparison, boolean interpolated,
                             WrapMode wrapMode, Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class TextureCubeMapImpl extends TextureImpl
            implements TextureCubeMap {
        public TextureCubeMapImpl(TextureHandle handle, FullFormat format, int width,
                                  int height, int depth, Vector4 borderColor,
                                  double anisotropicFiltering,
                                  Renderer.Comparison depthComparison,
                                  boolean interpolated, WrapMode wrapMode,
                                  Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class Texture1DArrayImpl extends TextureImpl
            implements Texture1DArray {
        public Texture1DArrayImpl(TextureHandle handle, FullFormat format, int width,
                                  int height, int depth, Vector4 borderColor,
                                  double anisotropicFiltering,
                                  Renderer.Comparison depthComparison,
                                  boolean interpolated, WrapMode wrapMode,
                                  Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class Texture2DArrayImpl extends TextureImpl
            implements Texture2DArray {
        public Texture2DArrayImpl(TextureHandle handle, FullFormat format, int width,
                                  int height, int depth, Vector4 borderColor,
                                  double anisotropicFiltering,
                                  Renderer.Comparison depthComparison,
                                  boolean interpolated, WrapMode wrapMode,
                                  Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class DepthMap2DImpl extends TextureImpl implements DepthMap2D {
        public DepthMap2DImpl(TextureHandle handle, FullFormat format, int width,
                              int height, int depth, Vector4 borderColor,
                              double anisotropicFiltering,
                              Renderer.Comparison depthComparison, boolean interpolated,
                              WrapMode wrapMode, Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }

    private static class DepthCubeMapImpl extends TextureImpl implements DepthCubeMap {
        public DepthCubeMapImpl(TextureHandle handle, FullFormat format, int width,
                                int height, int depth, Vector4 borderColor,
                                double anisotropicFiltering,
                                Renderer.Comparison depthComparison, boolean interpolated,
                                WrapMode wrapMode, Object[][] dataArrays) {
            super(handle, format, width, height, depth, borderColor, anisotropicFiltering,
                  depthComparison, interpolated, wrapMode, dataArrays);
        }
    }
}

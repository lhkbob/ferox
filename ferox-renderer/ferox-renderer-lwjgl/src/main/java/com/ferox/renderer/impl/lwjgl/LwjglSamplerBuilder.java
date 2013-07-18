package com.ferox.renderer.impl.lwjgl;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.builder.SamplerBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.AbstractSamplerBuilder;
import com.ferox.renderer.impl.resources.TextureImpl;

import java.nio.ByteBuffer;

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
        return 0;
    }

    @Override
    protected void pushImage(OpenGLContext context, int image, int mipmap, ByteBuffer imageData,
                             TextureImpl.FullFormat format, int width, int height, int depth) {
    }

    @Override
    protected void setBorderColor(OpenGLContext context, @Const Vector4 borderColor) {
    }

    @Override
    protected void setAnisotropy(OpenGLContext context, double anisotropy) {
    }

    @Override
    protected void setWrapMode(OpenGLContext context, Sampler.WrapMode mode) {
    }

    @Override
    protected void setInterpolated(OpenGLContext context, boolean interpolated, boolean hasMipmaps) {
    }

    @Override
    protected void setMipmapRange(OpenGLContext context, int base, int max) {
    }

    @Override
    protected void setDepthComparison(OpenGLContext context, Renderer.Comparison comparison) {
    }

}

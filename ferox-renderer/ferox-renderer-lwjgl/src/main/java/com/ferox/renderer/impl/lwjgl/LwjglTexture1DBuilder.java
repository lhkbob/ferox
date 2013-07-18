package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Texture1D;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.Texture1DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture1DBuilder extends LwjglSamplerBuilder<Texture1D, Texture1DBuilder>
        implements Texture1DBuilder {
    public LwjglTexture1DBuilder(FrameworkImpl framework) {
        super(Texture1DBuilder.class, Texture1D.class, TextureImpl.Target.TEX_1D, framework);
    }

    @Override
    protected Texture1D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture1D(handle);
    }

    @Override
    public SingleImageBuilder<Texture1D, BasicColorData> r() {
        return singleR();
    }

    @Override
    public SingleImageBuilder<Texture1D, BasicColorData> rg() {
        return singleRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture1D, RGBData> rgb() {
        return (SingleImageBuilder) singleRGB();
    }

    @Override
    public SingleImageBuilder<Texture1D, BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture1D, BasicColorData> rgba() {
        return (SingleImageBuilder) singleRGBA();
    }

    @Override
    public SingleImageBuilder<Texture1D, BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public SingleImageBuilder<Texture1D, ARGBData> argb() {
        return singleARGB();
    }
}

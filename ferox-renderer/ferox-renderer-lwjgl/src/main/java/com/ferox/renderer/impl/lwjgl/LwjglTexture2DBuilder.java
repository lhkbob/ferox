package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Texture2D;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.Texture2DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture2DBuilder extends LwjglSamplerBuilder<Texture2D, Texture2DBuilder>
        implements Texture2DBuilder {
    public LwjglTexture2DBuilder(FrameworkImpl framework) {
        super(Texture2DBuilder.class, Texture2D.class, TextureImpl.Target.TEX_2D, framework);
    }

    @Override
    protected Texture2D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture2D(handle);
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> r() {
        return singleR();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> rg() {
        return singleRG();
    }

    @Override
    public SingleImageBuilder<Texture2D, CompressedRGBData> rgb() {
        return singleRGB();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    public SingleImageBuilder<Texture2D, CompressedRGBAData> rgba() {
        return singleRGBA();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public SingleImageBuilder<Texture2D, ARGBData> argb() {
        return singleARGB();
    }
}

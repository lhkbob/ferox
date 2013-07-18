package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Texture3D;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.Texture3DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture3DBuilder extends LwjglSamplerBuilder<Texture3D, Texture3DBuilder>
        implements Texture3DBuilder {
    public LwjglTexture3DBuilder(FrameworkImpl framework) {
        super(Texture3DBuilder.class, Texture3D.class, TextureImpl.Target.TEX_3D, framework);
    }

    @Override
    protected Texture3D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture3D(handle);
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> r() {
        return singleR();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> rg() {
        return singleRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture3D, RGBData> rgb() {
        return (SingleImageBuilder) singleRGB();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture3D, BasicColorData> rgba() {
        return (SingleImageBuilder) singleRGBA();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public SingleImageBuilder<Texture3D, ARGBData> argb() {
        return singleARGB();
    }
}

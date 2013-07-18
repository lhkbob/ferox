package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Texture1DArray;
import com.ferox.renderer.builder.ArrayImageBuilder;
import com.ferox.renderer.builder.Texture1DArrayBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture1DArrayBuilder extends LwjglSamplerBuilder<Texture1DArray, Texture1DArrayBuilder>
        implements Texture1DArrayBuilder {
    public LwjglTexture1DArrayBuilder(FrameworkImpl framework) {
        super(Texture1DArrayBuilder.class, Texture1DArray.class, TextureImpl.Target.TEX_1D_ARRAY, framework);
    }

    @Override
    protected Texture1DArray wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture1DArray(handle);
    }

    @Override
    public ArrayImageBuilder<Texture1DArray, BasicColorData> r() {
        return arrayR();
    }

    @Override
    public ArrayImageBuilder<Texture1DArray, BasicColorData> rg() {
        return arrayRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture1DArray, RGBData> rgb() {
        return (ArrayImageBuilder) arrayRGB();
    }

    @Override
    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgr() {
        return arrayBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture1DArray, BasicColorData> rgba() {
        return (ArrayImageBuilder) arrayRGBA();
    }

    @Override
    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgra() {
        return arrayBGRA();
    }

    @Override
    public ArrayImageBuilder<Texture1DArray, ARGBData> argb() {
        return arrayARGB();
    }
}

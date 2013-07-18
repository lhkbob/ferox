package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Texture2DArray;
import com.ferox.renderer.builder.ArrayImageBuilder;
import com.ferox.renderer.builder.Texture2DArrayBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture2DArrayBuilder extends LwjglSamplerBuilder<Texture2DArray, Texture2DArrayBuilder>
        implements Texture2DArrayBuilder {
    public LwjglTexture2DArrayBuilder(FrameworkImpl framework) {
        super(Texture2DArrayBuilder.class, Texture2DArray.class, TextureImpl.Target.TEX_2D_ARRAY, framework);
    }

    @Override
    protected Texture2DArray wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture2DArray(handle);
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> r() {
        return arrayR();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rg() {
        return arrayRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture2DArray, RGBData> rgb() {
        return (ArrayImageBuilder) arrayRGB();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgr() {
        return arrayBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rgba() {
        return (ArrayImageBuilder) arrayRGBA();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgra() {
        return arrayBGRA();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, ARGBData> argb() {
        return arrayARGB();
    }
}

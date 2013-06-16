package com.ferox.renderer.builder;

import com.ferox.renderer.Texture1D;

/**
 *
 */
public interface Texture1DBuilder extends TextureBuilder<Texture1DBuilder> {
    public Texture1DBuilder length(int length);

    public SingleImageBuilder<Texture1D, BasicColorData> r();

    public SingleImageBuilder<Texture1D, BasicColorData> rg();

    public SingleImageBuilder<Texture1D, RGBData> rgb();

    public SingleImageBuilder<Texture1D, BasicColorData> bgr();

    public SingleImageBuilder<Texture1D, BasicColorData> rgba();

    public SingleImageBuilder<Texture1D, BasicColorData> bgra();

    public SingleImageBuilder<Texture1D, ARGBData> argb();
}

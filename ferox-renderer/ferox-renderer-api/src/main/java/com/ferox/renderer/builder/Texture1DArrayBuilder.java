package com.ferox.renderer.builder;

import com.ferox.renderer.Texture1DArray;

/**
 *
 */
public interface Texture1DArrayBuilder extends TextureBuilder<Texture1DArrayBuilder> {
    public Texture1DArrayBuilder length(int length);

    public Texture1DArrayBuilder imageCount(int length);

    public ArrayImageBuilder<Texture1DArray, BasicColorData> r();

    public ArrayImageBuilder<Texture1DArray, BasicColorData> rg();

    public ArrayImageBuilder<Texture1DArray, RGBData> rgb();

    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgr();

    public ArrayImageBuilder<Texture1DArray, BasicColorData> rgba();

    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgra();

    public ArrayImageBuilder<Texture1DArray, ARGBData> argb();
}

package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2DArray;

/**
 *
 */
public interface Texture2DArrayBuilder extends TextureBuilder<Texture2DArrayBuilder> {
    public Texture2DArrayBuilder width(int width);

    public Texture2DArrayBuilder height(int height);

    public Texture2DArrayBuilder imageCount(int length);

    public ArrayImageBuilder<Texture2DArray, BasicColorData> r();

    public ArrayImageBuilder<Texture2DArray, BasicColorData> rg();

    public ArrayImageBuilder<Texture2DArray, RGBData> rgb();

    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgr();

    public ArrayImageBuilder<Texture2DArray, BasicColorData> rgba();

    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgra();

    public ArrayImageBuilder<Texture2DArray, ARGBData> argb();
}

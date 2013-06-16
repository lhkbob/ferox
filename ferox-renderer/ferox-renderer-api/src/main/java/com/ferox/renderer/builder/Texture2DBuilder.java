package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2D;

/**
 *
 */
public interface Texture2DBuilder extends TextureBuilder<Texture2DBuilder> {
    public Texture2DBuilder width(int width);

    public Texture2DBuilder height(int height);

    public SingleImageBuilder<Texture2D, BasicColorData> r();

    public SingleImageBuilder<Texture2D, BasicColorData> rg();

    public SingleImageBuilder<Texture2D, CompressedRGBData> rgb();

    public SingleImageBuilder<Texture2D, BasicColorData> bgr();

    public SingleImageBuilder<Texture2D, CompressedRGBAData> rgba();

    public SingleImageBuilder<Texture2D, BasicColorData> bgra();

    public SingleImageBuilder<Texture2D, ARGBData> argb();
}

package com.ferox.renderer.builder;

import com.ferox.renderer.Texture3D;

/**
 *
 */
public interface Texture3DBuilder extends TextureBuilder<Texture3DBuilder> {
    public Texture3DBuilder width(int width);

    public Texture3DBuilder height(int height);

    public Texture3DBuilder depth(int depth);

    public SingleImageBuilder<Texture3D, BasicColorData> r();

    public SingleImageBuilder<Texture3D, BasicColorData> rg();

    public SingleImageBuilder<Texture3D, RGBData> rgb();

    public SingleImageBuilder<Texture3D, BasicColorData> bgr();

    public SingleImageBuilder<Texture3D, BasicColorData> rgba();

    public SingleImageBuilder<Texture3D, BasicColorData> bgra();

    public SingleImageBuilder<Texture3D, ARGBData> argb();
}

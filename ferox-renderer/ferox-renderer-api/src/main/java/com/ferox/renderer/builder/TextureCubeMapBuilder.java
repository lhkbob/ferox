package com.ferox.renderer.builder;

import com.ferox.renderer.TextureCubeMap;

/**
 *
 */
public interface TextureCubeMapBuilder extends TextureBuilder<TextureCubeMapBuilder> {
    public TextureCubeMapBuilder side(int side);

    public CubeImageBuilder<TextureCubeMap, BasicColorData> r();

    public CubeImageBuilder<TextureCubeMap, BasicColorData> rg();

    public CubeImageBuilder<TextureCubeMap, CompressedRGBData> rgb();

    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgr();

    public CubeImageBuilder<TextureCubeMap, CompressedRGBAData> rgba();

    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgra();

    public CubeImageBuilder<TextureCubeMap, ARGBData> argb();
}

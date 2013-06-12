package com.ferox.renderer.builder;

import com.ferox.renderer.TextureCubeMap;

/**
 *
 */
public interface TextureCubeMapBuilder extends TextureBuilder<TextureCubeMapBuilder> {
    public TextureCubeMapBuilder side(int side);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder extends
                                          CubeImageBuilder<TextureCubeMap, TextureBuilder.BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder extends
                                           CubeImageBuilder<TextureCubeMap, TextureBuilder.BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder extends
                                            CubeImageBuilder<TextureCubeMap, TextureBuilder.CompressedRGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder extends
                                            CubeImageBuilder<TextureCubeMap, TextureBuilder.BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder extends
                                             CubeImageBuilder<TextureCubeMap, TextureBuilder.CompressedRGBAData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder extends
                                             CubeImageBuilder<TextureCubeMap, TextureBuilder.BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder extends
                                             CubeImageBuilder<TextureCubeMap, TextureBuilder.ARGBData<ARGBImageBuilder>> {
    }
}

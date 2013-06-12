package com.ferox.resource.builder;

import com.ferox.resource.Texture1DArray;

/**
 *
 */
public interface Texture1DArrayBuilder extends TextureBuilder<Texture1DArrayBuilder> {
    public Texture1DArrayBuilder length(int length);

    public Texture1DArrayBuilder imageCount(int length);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder extends
                                          ArrayImageBuilder<Texture1DArray, TextureBuilder.BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder extends
                                           ArrayImageBuilder<Texture1DArray, TextureBuilder.BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder extends
                                            ArrayImageBuilder<Texture1DArray, TextureBuilder.RGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder extends
                                            ArrayImageBuilder<Texture1DArray, TextureBuilder.BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder extends
                                             ArrayImageBuilder<Texture1DArray, TextureBuilder.BasicColorData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder extends
                                             ArrayImageBuilder<Texture1DArray, TextureBuilder.BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder extends
                                             ArrayImageBuilder<Texture1DArray, TextureBuilder.ARGBData<ARGBImageBuilder>> {
    }
}

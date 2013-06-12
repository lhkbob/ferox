package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2DArray;

/**
 *
 */
public interface Texture2DArrayBuilder extends TextureBuilder<Texture2DArrayBuilder> {
    public Texture2DArrayBuilder width(int width);

    public Texture2DArrayBuilder height(int height);

    public Texture2DArrayBuilder imageCount(int length);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder extends
                                          ArrayImageBuilder<Texture2DArray, TextureBuilder.BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder extends
                                           ArrayImageBuilder<Texture2DArray, TextureBuilder.BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder extends
                                            ArrayImageBuilder<Texture2DArray, TextureBuilder.RGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder extends
                                            ArrayImageBuilder<Texture2DArray, TextureBuilder.BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder extends
                                             ArrayImageBuilder<Texture2DArray, TextureBuilder.BasicColorData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder extends
                                             ArrayImageBuilder<Texture2DArray, TextureBuilder.BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder extends
                                             ArrayImageBuilder<Texture2DArray, TextureBuilder.ARGBData<ARGBImageBuilder>> {
    }
}

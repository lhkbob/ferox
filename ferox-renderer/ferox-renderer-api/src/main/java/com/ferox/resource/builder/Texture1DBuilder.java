package com.ferox.resource.builder;

import com.ferox.resource.Texture1D;

/**
 *
 */
public interface Texture1DBuilder extends TextureBuilder<Texture1DBuilder> {
    public Texture1DBuilder length(int length);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder extends
                                          SingleImageBuilder<Texture1D, TextureBuilder.BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder extends
                                           SingleImageBuilder<Texture1D, TextureBuilder.BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder extends
                                            SingleImageBuilder<Texture1D, TextureBuilder.RGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder extends
                                            SingleImageBuilder<Texture1D, TextureBuilder.BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder extends
                                             SingleImageBuilder<Texture1D, TextureBuilder.BasicColorData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder extends
                                             SingleImageBuilder<Texture1D, TextureBuilder.BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder extends
                                             SingleImageBuilder<Texture1D, TextureBuilder.ARGBData<ARGBImageBuilder>> {
    }
}

package com.ferox.resource.builder;

import com.ferox.resource.Texture3D;

/**
 *
 */
public interface Texture3DBuilder extends TextureBuilder<Texture3DBuilder> {
    public Texture3DBuilder width(int width);

    public Texture3DBuilder height(int height);

    public Texture3DBuilder depth(int depth);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder extends
                                          SingleImageBuilder<Texture3D, TextureBuilder.BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder extends
                                           SingleImageBuilder<Texture3D, TextureBuilder.BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder extends
                                            SingleImageBuilder<Texture3D, TextureBuilder.RGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder extends
                                            SingleImageBuilder<Texture3D, TextureBuilder.BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder extends
                                             SingleImageBuilder<Texture3D, TextureBuilder.BasicColorData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder extends
                                             SingleImageBuilder<Texture3D, TextureBuilder.BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder extends
                                             SingleImageBuilder<Texture3D, TextureBuilder.ARGBData<ARGBImageBuilder>> {
    }
}

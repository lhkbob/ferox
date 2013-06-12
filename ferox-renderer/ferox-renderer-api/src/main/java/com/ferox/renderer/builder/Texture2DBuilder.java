package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2D;

/**
 *
 */
public interface Texture2DBuilder extends TextureBuilder<Texture2DBuilder> {
    public Texture2DBuilder width(int width);

    public Texture2DBuilder height(int height);

    public RImageBuilder r();

    public RGImageBuilder rg();

    public RGBImageBuilder rgb();

    public BGRImageBuilder bgr();

    public RGBAImageBuilder rgba();

    public BGRAImageBuilder bgra();

    public ARGBImageBuilder argb();

    public static interface RImageBuilder
            extends SingleImageBuilder<Texture2D, BasicColorData<RImageBuilder>> {
    }

    public static interface RGImageBuilder
            extends SingleImageBuilder<Texture2D, BasicColorData<RGImageBuilder>> {
    }

    public static interface RGBImageBuilder
            extends SingleImageBuilder<Texture2D, CompressedRGBData<RGBImageBuilder>> {
    }

    public static interface BGRImageBuilder
            extends SingleImageBuilder<Texture2D, BasicColorData<BGRImageBuilder>> {
    }

    public static interface RGBAImageBuilder
            extends SingleImageBuilder<Texture2D, CompressedRGBAData<RGBAImageBuilder>> {
    }

    public static interface BGRAImageBuilder
            extends SingleImageBuilder<Texture2D, BasicColorData<BGRAImageBuilder>> {
    }

    public static interface ARGBImageBuilder
            extends SingleImageBuilder<Texture2D, ARGBData<ARGBImageBuilder>> {
    }
}

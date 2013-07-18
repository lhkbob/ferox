package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.TextureCubeMap;
import com.ferox.renderer.builder.CubeImageBuilder;
import com.ferox.renderer.builder.TextureCubeMapBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTextureCubeMapBuilder extends LwjglSamplerBuilder<TextureCubeMap, TextureCubeMapBuilder>
        implements TextureCubeMapBuilder {
    public LwjglTextureCubeMapBuilder(FrameworkImpl framework) {
        super(TextureCubeMapBuilder.class, TextureCubeMap.class, TextureImpl.Target.TEX_CUBEMAP, framework);
    }

    @Override
    protected TextureCubeMap wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTextureCubeMap(handle);
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, BasicColorData> r() {
        return cubeR();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, BasicColorData> rg() {
        return cubeRG();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, CompressedRGBData> rgb() {
        return cubeRGB();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgr() {
        return cubeBGR();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, CompressedRGBAData> rgba() {
        return cubeRGBA();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgra() {
        return cubeBGRA();
    }

    @Override
    public CubeImageBuilder<TextureCubeMap, ARGBData> argb() {
        return cubeARGB();
    }
}

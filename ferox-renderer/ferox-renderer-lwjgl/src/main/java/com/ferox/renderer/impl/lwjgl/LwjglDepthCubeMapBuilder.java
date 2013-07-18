package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DepthCubeMap;
import com.ferox.renderer.builder.CubeImageBuilder;
import com.ferox.renderer.builder.DepthCubeMapBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglDepthCubeMapBuilder extends LwjglSamplerBuilder<DepthCubeMap, DepthCubeMapBuilder>
        implements DepthCubeMapBuilder {
    public LwjglDepthCubeMapBuilder(FrameworkImpl framework) {
        super(DepthCubeMapBuilder.class, DepthCubeMap.class, TextureImpl.Target.TEX_CUBEMAP, framework);
    }

    @Override
    protected DepthCubeMap wrap(TextureImpl.TextureHandle handle) {
        return wrapAsDepthCubeMap(handle);
    }

    @Override
    public CubeImageBuilder<DepthCubeMap, DepthData> depth() {
        return cubeDepth();
    }

    @Override
    public CubeImageBuilder<DepthCubeMap, DepthStencilData> depthStencil() {
        return cubeDepthStencil();
    }
}

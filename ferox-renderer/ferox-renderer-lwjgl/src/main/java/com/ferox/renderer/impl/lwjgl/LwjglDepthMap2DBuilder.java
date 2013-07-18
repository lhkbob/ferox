package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.DepthMap2D;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglDepthMap2DBuilder extends LwjglSamplerBuilder<DepthMap2D, DepthMap2DBuilder>
        implements DepthMap2DBuilder {
    public LwjglDepthMap2DBuilder(FrameworkImpl framework) {
        super(DepthMap2DBuilder.class, DepthMap2D.class, TextureImpl.Target.TEX_2D, framework);
    }

    @Override
    protected DepthMap2D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsDepthMap2D(handle);
    }

    @Override
    public SingleImageBuilder<DepthMap2D, DepthData> depth() {
        return singleDepth();
    }

    @Override
    public SingleImageBuilder<DepthMap2D, DepthStencilData> depthStencil() {
        return singleDepthStencil();
    }
}

package com.ferox.resource.builder;

import com.ferox.renderer.Renderer;

/**
 *
 */
public interface DepthMapBuilder<B extends DepthMapBuilder<B>> extends SamplerBuilder<B> {
    public B depthComparison(Renderer.Comparison compare);

    public static interface DepthData<I> {
        public I from(float[] data);

        public I fromUnsigned(int[] data);
    }

    public static interface DepthStencilData<I> {
        public I fromBits(int[] data);
    }
}

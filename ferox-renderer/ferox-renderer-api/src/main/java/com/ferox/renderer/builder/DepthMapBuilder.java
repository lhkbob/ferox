package com.ferox.renderer.builder;

import com.ferox.renderer.Renderer;

/**
 *
 */
public interface DepthMapBuilder<B extends DepthMapBuilder<B>> extends SamplerBuilder<B> {
    public B depthComparison(Renderer.Comparison compare);

    public B borderDepth(double depth);

    public static interface DepthData<I> {
        public I from(float[] data);

        public I fromUnsignedNormalized(int[] data);

        public I fromUnsignedNormalized(short[] data);
    }

    public static interface DepthStencilData<I> {
        public I fromBits(int[] data);
    }
}

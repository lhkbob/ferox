package com.ferox.renderer.builder;

import com.ferox.renderer.Renderer;

/**
 *
 */
public interface DepthMapBuilder<B extends DepthMapBuilder<B>> extends SamplerBuilder<B> {
    public B depthComparison(Renderer.Comparison compare);

    public B borderDepth(double depth);

    public static interface DepthData {
        public void from(float[] data);

        public void fromUnsignedNormalized(int[] data);

        public void fromUnsignedNormalized(short[] data);
    }

    public static interface DepthStencilData {
        public void fromBits(int[] data);
    }
}

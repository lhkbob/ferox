package com.ferox.renderer;

/**
 *
 */
public interface DepthMap extends Sampler {
    public boolean isDepthComparisonEnabled();

    public Renderer.Comparison getDepthComparison();
}

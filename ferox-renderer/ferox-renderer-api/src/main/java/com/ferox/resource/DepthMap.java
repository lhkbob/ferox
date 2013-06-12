package com.ferox.resource;

import com.ferox.renderer.Renderer;

/**
 *
 */
public interface DepthMap extends Sampler {
    public boolean isDepthComparisonEnabled();

    public Renderer.Comparison getDepthComparison();
}

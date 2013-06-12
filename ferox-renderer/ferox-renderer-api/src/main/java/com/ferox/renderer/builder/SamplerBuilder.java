package com.ferox.renderer.builder;

import com.ferox.renderer.Sampler;

/**
 *
 */
public interface SamplerBuilder<B extends SamplerBuilder<B>> {
    public B dynamic();

    public B interpolated();

    public B wrap(Sampler.WrapMode wrap);
}

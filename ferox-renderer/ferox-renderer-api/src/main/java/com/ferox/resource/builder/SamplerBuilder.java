package com.ferox.resource.builder;

import com.ferox.resource.Sampler;

/**
 *
 */
public interface SamplerBuilder<B extends SamplerBuilder<B>> {
    public B dynamic();

    public B interpolated();

    public B wrap(Sampler.WrapMode wrap);
}

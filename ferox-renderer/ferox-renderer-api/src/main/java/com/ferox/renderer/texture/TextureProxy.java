package com.ferox.renderer.texture;

import com.ferox.renderer.Framework;
import com.ferox.renderer.Sampler;

/**
 *
 */
public interface TextureProxy<T extends Sampler> {
    public T convert(Framework framework);
}

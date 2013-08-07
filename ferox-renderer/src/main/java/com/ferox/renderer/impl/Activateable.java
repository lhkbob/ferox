package com.ferox.renderer.impl;

/**
 *
 */
public interface Activateable {
    /**
     * Notify the renderer that the provided surface has been activated and will be using this Renderer.
     *
     * @param active The now active surface
     */
    public void activate(AbstractSurface active);
}

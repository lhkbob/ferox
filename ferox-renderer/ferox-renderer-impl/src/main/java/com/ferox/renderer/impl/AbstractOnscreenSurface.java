package com.ferox.renderer.impl;

import com.ferox.renderer.OnscreenSurface;

/**
 * AbstractOnscreenSurface combines AbstractSurface with OnscreenSurface so that
 * {@link SurfaceFactory} can return a properly typed OnscreenSurface that will
 * work with AbstractFramework implementations. It does not add any additional
 * functionality or help with the implementation of OnscreenSurface.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractOnscreenSurface extends AbstractSurface implements OnscreenSurface {
    public AbstractOnscreenSurface(AbstractFramework framework) {
        super(framework);
    }
}

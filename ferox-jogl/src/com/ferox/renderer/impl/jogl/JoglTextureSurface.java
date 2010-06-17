package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.TextureSurfaceDelegate;
import com.ferox.resource.Texture;

/**
 * JoglTextureSurface is a functioning implementation of AbstractTextureSurface
 * that relies on either an {@link FboSurfaceDelegate} or a
 * {@link PbufferSurfaceDelegate} depending on the hardware capabilities of the
 * host computer.
 * 
 * @author Michael Ludwig
 */
public class JoglTextureSurface extends AbstractTextureSurface {
    public JoglTextureSurface(JoglFramework framework, TextureSurfaceOptions options) {
        super(framework, options);
    }
    
    @Override
    protected TextureSurfaceDelegate createDelegate(Texture[] colors, Texture depth, TextureSurfaceOptions options) {
        RenderCapabilities caps = getFramework().getCapabilities();
        if (caps.getFboSupport())
            return new FboSurfaceDelegate((JoglFramework) framework, colors, depth);
        else
            return new PbufferSurfaceDelegate((JoglFramework) framework, colors, depth, lock);
    }
}

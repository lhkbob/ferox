package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.TextureSurfaceDelegate;
import com.ferox.resource.Texture;

public class LwjglTextureSurface extends AbstractTextureSurface {

    public LwjglTextureSurface(Framework framework, TextureSurfaceOptions options) {
        super(framework, options);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected TextureSurfaceDelegate createDelegate(Texture[] colors, Texture depth, TextureSurfaceOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

}

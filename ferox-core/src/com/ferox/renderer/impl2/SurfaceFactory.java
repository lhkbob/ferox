package com.ferox.renderer.impl2;

import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.TextureSurfaceOptions;

public interface SurfaceFactory {
    public AbstractTextureSurface createTextureSurface(AbstractFramework framework, TextureSurfaceOptions options, 
                                                       OpenGLContextAdapter sharedContext);
    
    public AbstractOnscreenSurface createOnscreenSurface(AbstractFramework framework, OnscreenSurfaceOptions options, 
                                                         OpenGLContextAdapter sharedContext);
    
    public OpenGLContextAdapter createShadowContext(OpenGLContextAdapter sharedContext);
}

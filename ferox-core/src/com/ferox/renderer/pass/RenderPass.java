package com.ferox.renderer.pass;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.Surface;

public interface RenderPass<R extends Renderer> {
    public void render(R renderer, Surface surface);
    
    public Class<R> getRendererType();
}

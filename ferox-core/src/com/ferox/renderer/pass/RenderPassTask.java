package com.ferox.renderer.pass;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;

public class RenderPassTask implements Task<Void> {
    private final Surface surface;
    private final RenderPass<?>[] passes;
    
    public RenderPassTask(Surface surface, RenderPass<?>... passes) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        if (passes == null)
            throw new NullPointerException("Passes cannot be null");
        for (int i = 0; i < passes.length; i++) {
            if (passes[i] == null)
                throw new NullPointerException("Passes cannot have a null element, at: " + i);
        }
        
        this.surface = surface;
        this.passes = passes;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Void run(HardwareAccessLayer access) {
        Context context = null;
        Renderer renderer = null;
        
        for (int i = 0; i < passes.length; i++) {
            if (context == null || !passes[i].getRendererType().isInstance(renderer)) {
                context = access.setActiveSurface(surface);
                if (passes[i].getRendererType().equals(FixedFunctionRenderer.class)) {
                    renderer = context.getFixedFunctionRenderer();
                } else if (passes[i].getRendererType().equals(GlslRenderer.class)) {
                    renderer = context.getGlslRenderer();
                } else if (passes[i].getRendererType().equals(Renderer.class)) {
                    // this path is only followed if the pass is the 1st pass,
                    // otherwise isInstance() will return true and we'll reuse the old renderer
                    //  - for the first pass that won't work, so we pick first non-null renderer
                    renderer = context.getFixedFunctionRenderer();
                    if (renderer == null)
                        renderer = context.getGlslRenderer();
                } else
                    throw new RenderException("RenderPass requested renderer that was not Renderer, FixedFunctionRenderer or GlslRenderer");
            }
            
            if (renderer != null) {
                // generics doesn't really help us here, so we do some messy instance checks
                if (passes[i].getRendererType().equals(FixedFunctionRenderer.class))
                    ((RenderPass<FixedFunctionRenderer>) passes[i]).render((FixedFunctionRenderer) renderer, surface);
                else if (passes[i].getRendererType().equals(GlslRenderer.class))
                    ((RenderPass<GlslRenderer>) passes[i]).render((GlslRenderer) renderer, surface);
                else
                    ((RenderPass<Renderer>) passes[i]).render(renderer, surface);
            }
        }
        
        if (context != null)
            context.flush();
        return null;
    }
}

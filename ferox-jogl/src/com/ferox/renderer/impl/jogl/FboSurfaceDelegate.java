package com.ferox.renderer.impl.jogl;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.TextureSurfaceDelegate;
import com.ferox.resource.Texture;

public class FboSurfaceDelegate extends TextureSurfaceDelegate {
    private final JoglFramework framework;
    private final ConcurrentMap<JoglContext, FramebufferObject> fbos;
    
    public FboSurfaceDelegate(JoglFramework framework, Texture[] colorTextures, Texture depthTexture) {
        super(colorTextures, depthTexture);
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        this.framework = framework;
        fbos = new ConcurrentHashMap<JoglContext, FramebufferObject>();
    }

    @Override
    public void destroy() {
        for (Entry<JoglContext, FramebufferObject> e: fbos.entrySet())
            e.getKey().notifyFboZombie(e.getValue());
    }
    
    @Override
    public JoglContext getContext() {
        // no context to use
        return null;
    }

    @Override
    public void postRender(Action next) {
        // the only surface to have a null context in this framework
        // is a surface using an fbo, in which case the next setLayer()
        // will take care of unbinding this fbo
        if (next != null && ((AbstractSurface) next.getSurface()).getContext() == null)
            return;
        
        FramebufferObject fbo = fbos.get(JoglContext.getCurrent());
        if (fbo != null)
            fbo.release();
    }

    @Override
    public void setLayer(int layer, int depth) {
        JoglContext context = JoglContext.getCurrent();
        FramebufferObject fbo = fbos.get(context);
        if (fbo == null) {
            fbo = new FramebufferObject(framework, getColorBuffers(), getDepthBuffer());
            fbos.put(context, fbo);
        }
        
        fbo.bind(layer, depth);
    }
}

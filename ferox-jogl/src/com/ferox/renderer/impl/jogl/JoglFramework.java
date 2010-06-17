package com.ferox.renderer.impl.jogl;

import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GLProfile;
import javax.media.opengl.Threading;

import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.DefaultRenderManager;
import com.ferox.renderer.impl.DefaultResourceManager;
import com.ferox.renderer.impl.RenderManager;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.resource.ResourceDriver;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture;

public abstract class JoglFramework extends AbstractFramework {
    private final GLProfile profile;
    private final JoglContext shadowContext;
    
    public JoglFramework(GLProfile profile, int capForceBits, boolean useShadowContext) {
        if (profile == null)
            throw new NullPointerException("GLProfile cannot be null");
        this.profile = profile;

        if (Threading.isSingleThreaded())
            Threading.disableSingleThreading();
        
        RenderCapabilities caps = new JoglRenderCapabilities(profile, capForceBits);
        
        Map<Class<? extends Resource>, ResourceDriver> drivers = new HashMap<Class<? extends Resource>, ResourceDriver>();
        drivers.put(Geometry.class, new JoglGeometryDriver(caps));
        drivers.put(Texture.class, new JoglTextureDriver(caps));
        
        ResourceManager res = new DefaultResourceManager(drivers);
        RenderManager render = new DefaultRenderManager(res);
        
        if (useShadowContext) {
            if (caps.getPbufferSupport())
                shadowContext = PbufferShadowContext.create(this, profile);
            else
                shadowContext = OnscreenShadowContext.create(this, profile);
        } else
            shadowContext = null;
        res.setContext(shadowContext);
        init(res, render, caps);
    }
    
    public GLProfile getProfile() {
        return profile;
    }

    @Override
    protected OnscreenSurface createSurfaceImpl(OnscreenSurfaceOptions options) {
        return new JoglOnscreenSurface(this, options);
    }

    @Override
    protected TextureSurface createSurfaceImpl(TextureSurfaceOptions options) {
        return new JoglTextureSurface(this, options);
    }

    @Override
    protected void destroyImpl() {
        if (shadowContext != null)
            shadowContext.destroy();
    }
    
    protected abstract AbstractRenderer createRenderer();
}

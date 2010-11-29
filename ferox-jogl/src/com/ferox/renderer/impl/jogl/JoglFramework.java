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
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.DefaultRenderManager;
import com.ferox.renderer.impl.DefaultResourceManager;
import com.ferox.renderer.impl.FixedFunctionGlslRenderer;
import com.ferox.renderer.impl.RenderManager;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.resource.ResourceDriver;
import com.ferox.resource.Geometry;
import com.ferox.resource.GlslShader;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture;

public class JoglFramework extends AbstractFramework {
    private final GLProfile profile;
    private final JoglContext shadowContext;
    private final boolean emulateFfp;
    
    public JoglFramework() {
        this(false);
    }
    
    public JoglFramework(boolean emulateFixedFunction) {
        this(emulateFixedFunction, false, false);
    }
    
    public JoglFramework(boolean emulateFixedFunction, boolean forceNoBackgroundContext) {
        this(emulateFixedFunction, forceNoBackgroundContext, false);
    }
    
    public JoglFramework(boolean emulateFixedFunction, boolean forceNoBackgroundContext, boolean forceNoPbuffers) {
        if (GLProfile.isGL3Available())
            profile = GLProfile.get(GLProfile.GL3);
        else if (GLProfile.isGL2Available())
            profile = GLProfile.get(GLProfile.GL2);
        else
            throw new RuntimeException("Minimum required GL profile of GL2 is not available");
        
        if (Threading.isSingleThreaded())
            Threading.disableSingleThreading();
        
        int capForceBits = (forceNoPbuffers ? JoglRenderCapabilities.FORCE_NO_PBUFFER : 0);
        RenderCapabilities caps = new JoglRenderCapabilities(profile, capForceBits);
        
        emulateFfp = caps.hasGlslRenderer() && (emulateFixedFunction || caps.getVersion() >= 3.0f);
        
        Map<Class<? extends Resource>, ResourceDriver> drivers = new HashMap<Class<? extends Resource>, ResourceDriver>();
        drivers.put(Geometry.class, new JoglGeometryDriver(caps));
        drivers.put(Texture.class, new JoglTextureDriver(caps));
        
        if (caps.hasGlslRenderer())
            drivers.put(GlslShader.class, new JoglGlslShaderDriver(caps));
        
        ResourceManager res = new DefaultResourceManager(drivers);
        RenderManager render = new DefaultRenderManager(res);
        
        init(res, render, caps);
        
        if (!forceNoBackgroundContext) {
            if (caps.getPbufferSupport())
                shadowContext = PbufferShadowContext.create(this, profile);
            else
                shadowContext = OnscreenShadowContext.create(this, profile);
        } else
            shadowContext = null;
        res.setContext(shadowContext);
    }
    
    public GLProfile getProfile() {
        return profile;
    }
    
    public boolean isFixedFunctionEmulated() {
        return emulateFfp;
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
    
    protected AbstractGlslRenderer createGlslRenderer() {
        if (getCapabilities().hasGlslRenderer())
            return new JoglGlslRenderer(this);
        else
            return null;
    }
    
    protected AbstractFixedFunctionRenderer createFixedFunctionRenderer() {
        if (emulateFfp)
            return new FixedFunctionGlslRenderer(createGlslRenderer());
        else
            return new JoglFixedFunctionRenderer(this);
    }
}

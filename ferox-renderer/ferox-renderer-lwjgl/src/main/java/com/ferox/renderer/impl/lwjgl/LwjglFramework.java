package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.ResourceDriver;

public class LwjglFramework extends AbstractFramework {
    private LwjglFramework(LwjglSurfaceFactory factory, ResourceDriver... drivers) {
        super(factory, drivers);
    }
    
    public static LwjglFramework create() {
        return create(2);
    }
    
    public static LwjglFramework create(int numThreads) {
        return create(false, false, false, false);
    }
    
    public static LwjglFramework create(boolean forceNoFfp, boolean forceNoGlsl) {
        return create(false, false);
    }
    
    public static LwjglFramework create(boolean forceNoFfp, boolean forceNoGlsl,
                                        boolean forceNoPbuffers, boolean forceNoFbos) {
        int capBits = 0;
        if (forceNoGlsl)
            capBits |= LwjglRenderCapabilities.FORCE_NO_GLSL;
        if (forceNoPbuffers)
            capBits |= LwjglRenderCapabilities.FORCE_NO_PBUFFER;
        if (forceNoFbos)
            capBits |= LwjglRenderCapabilities.FORCE_NO_FBO;
        
        // FIXME: how to handle forceNoFfp?
        
        LwjglSurfaceFactory factory = new LwjglSurfaceFactory(capBits);
        LwjglFramework framework = new LwjglFramework(factory, 
                                                      new LwjglTextureResourceDriver(),
                                                      new LwjglVertexBufferObjectResourceDriver(),
                                                      new LwjglGlslShaderResourceDriver());
        
        framework.initialize();
        return framework;
    }
}

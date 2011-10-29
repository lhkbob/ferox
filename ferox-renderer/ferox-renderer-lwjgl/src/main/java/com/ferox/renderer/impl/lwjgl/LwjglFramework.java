package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.ResourceDriver;

public class LwjglFramework extends AbstractFramework {
    private LwjglFramework(LwjglSurfaceFactory factory, int numThreads, ResourceDriver<?>... drivers) {
        super(factory, numThreads, drivers);
    }
    
    public static LwjglFramework create() {
        return create(2);
    }
    
    public static LwjglFramework create(int numThreads) {
        return create(numThreads, false, false);
    }
    
    public static LwjglFramework create(int numThreads,
                                       boolean forceNoFfp, boolean forceNoGlsl) {
        return create(numThreads, forceNoFfp, forceNoGlsl, false, false);
    }

    public static LwjglFramework create(int numThreads,
                                       boolean forceNoFfp, boolean forceNoGlsl,
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
        LwjglFramework framework = new LwjglFramework(factory, numThreads, 
                                                      new LwjglTextureResourceDriver(),
                                                      new LwjglVertexBufferObjectResourceDriver(),
                                                      new LwjglGlslShaderResourceDriver());
        
        framework.initialize();
        return framework;
    }
}

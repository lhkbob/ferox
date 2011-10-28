package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;
import javax.media.opengl.Threading;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.ResourceDriver;

public class JoglFramework extends AbstractFramework {
    private JoglFramework(JoglSurfaceFactory factory, int numThreads, ResourceDriver<?>... drivers) {
        super(factory, numThreads, drivers);
    }
    
    public static JoglFramework create() {
        return create(2);
    }
    
    public static JoglFramework create(int numThreads) {
        return create(numThreads, false, false);
    }
    
    public static JoglFramework create(int numThreads,
                                       boolean forceNoFfp, boolean forceNoGlsl) {
        return create(numThreads, forceNoFfp, forceNoGlsl, false, false);
    }

    public static JoglFramework create(int numThreads,
                                       boolean forceNoFfp, boolean forceNoGlsl,
                                       boolean forceNoPbuffers, boolean forceNoFbos) {

        int capBits = 0;
        if (forceNoGlsl)
            capBits |= JoglRenderCapabilities.FORCE_NO_GLSL;
        if (forceNoPbuffers)
            capBits |= JoglRenderCapabilities.FORCE_NO_PBUFFER;
        if (forceNoFbos)
            capBits |= JoglRenderCapabilities.FORCE_NO_FBO;
        
        // FIXME: how to handle forceNoFfp?
        
        // FIXME: select profile better, based on properties?
        GLProfile profile;
        if (GLProfile.isGL3Available())
            profile = GLProfile.get(GLProfile.GL3);
        else if (GLProfile.isGL2Available())
            profile = GLProfile.get(GLProfile.GL2);
        else
            throw new RuntimeException("Minimum required GL profile of GL2 is not available");

        // Must configure JOGL to let us control the threading
        if (Threading.isSingleThreaded())
            Threading.disableSingleThreading();
        
        JoglSurfaceFactory factory = new JoglSurfaceFactory(profile, capBits);
        JoglFramework framework = new JoglFramework(factory, numThreads, 
                                                    new JoglTextureResourceDriver(),
                                                    new JoglVertexBufferObjectResourceDriver(),
                                                    new JoglGlslShaderResourceDriver());
        
        framework.initialize();
        return framework;
    }
}

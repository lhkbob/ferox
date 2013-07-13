/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.ResourceDriver;

import javax.media.opengl.GLProfile;
import javax.media.opengl.Threading;

public class JoglFramework extends FrameworkImpl {
    private JoglFramework(JoglSurfaceFactory factory, ResourceDriver... drivers) {
        super(factory, drivers);
    }

    public static JoglFramework create() {
        return create(false, false);
    }

    public static JoglFramework create(boolean forceNoFfp, boolean forceNoGlsl) {
        return create(forceNoFfp, forceNoGlsl, false, false);
    }

    public static JoglFramework create(boolean forceNoFfp, boolean forceNoGlsl, boolean forceNoPbuffers,
                                       boolean forceNoFbos) {

        int capBits = 0;
        if (forceNoGlsl) {
            capBits |= JoglRenderCapabilities.FORCE_NO_GLSL;
        }
        if (forceNoPbuffers) {
            capBits |= JoglRenderCapabilities.FORCE_NO_PBUFFER;
        }
        if (forceNoFbos) {
            capBits |= JoglRenderCapabilities.FORCE_NO_FBO;
        }

        // FIXME: how to handle forceNoFfp?

        // FIXME: select profile better, based on properties? can't select GL3
        // until we have GL3-only FFP implemented
        GLProfile profile;
        if (GLProfile.isAvailable(GLProfile.GL2)) {
            profile = GLProfile.get(GLProfile.GL2);
        } else {
            throw new RuntimeException("Minimum required GL profile of GL2 is not available");
        }

        // Must configure JOGL to let us control the threading
        if (Threading.isSingleThreaded()) {
            Threading.disableSingleThreading();
        }

        JoglSurfaceFactory factory = new JoglSurfaceFactory(profile, capBits);
        JoglFramework framework = new JoglFramework(factory, new JoglTextureResourceDriver(),
                                                    new JoglVertexBufferObjectResourceDriver(),
                                                    new JoglGlslShaderResourceDriver());

        framework.initialize();
        return framework;
    }
}

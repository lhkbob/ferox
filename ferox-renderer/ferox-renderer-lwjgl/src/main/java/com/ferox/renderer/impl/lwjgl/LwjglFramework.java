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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.ResourceDriver;

public class LwjglFramework extends AbstractFramework {
    private LwjglFramework(LwjglSurfaceFactory factory, ResourceDriver... drivers) {
        super(factory, drivers);
    }

    public static LwjglFramework create() {
        return create(false, false, false, false);
    }

    public static LwjglFramework create(boolean forceNoFfp, boolean forceNoGlsl) {
        return create(false, false);
    }

    public static LwjglFramework create(boolean forceNoFfp, boolean forceNoGlsl,
                                        boolean forceNoPbuffers, boolean forceNoFbos) {
        int capBits = 0;
        if (forceNoGlsl) {
            capBits |= LwjglRenderCapabilities.FORCE_NO_GLSL;
        }
        if (forceNoPbuffers) {
            capBits |= LwjglRenderCapabilities.FORCE_NO_PBUFFER;
        }
        if (forceNoFbos) {
            capBits |= LwjglRenderCapabilities.FORCE_NO_FBO;
        }

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

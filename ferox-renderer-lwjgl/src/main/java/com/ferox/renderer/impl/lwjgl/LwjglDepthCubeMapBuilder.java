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

import com.ferox.renderer.DepthCubeMap;
import com.ferox.renderer.builder.CubeImageData;
import com.ferox.renderer.builder.DepthCubeMapBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglDepthCubeMapBuilder extends LwjglSamplerBuilder<DepthCubeMap, DepthCubeMapBuilder>
        implements DepthCubeMapBuilder {
    public LwjglDepthCubeMapBuilder(FrameworkImpl framework) {
        super(DepthCubeMapBuilder.class, DepthCubeMap.class, TextureImpl.Target.TEX_CUBEMAP, framework);
        // preconfigure abstract builder for this type of texture
        depth(1);
        imageCount(6);
    }

    @Override
    protected DepthCubeMap wrap(TextureImpl.TextureHandle handle) {
        return wrapAsDepthCubeMap(handle);
    }

    @Override
    public CubeImageData<? extends DepthData> depth() {
        return cubeDepth();
    }

    @Override
    public CubeImageData<? extends DepthStencilData> depthStencil() {
        return cubeDepthStencil();
    }
}

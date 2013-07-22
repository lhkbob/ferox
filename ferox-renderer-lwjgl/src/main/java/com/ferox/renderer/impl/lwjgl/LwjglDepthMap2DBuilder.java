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

import com.ferox.renderer.DepthMap2D;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.builder.ImageData;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglDepthMap2DBuilder extends LwjglSamplerBuilder<DepthMap2D, DepthMap2DBuilder>
        implements DepthMap2DBuilder {
    public LwjglDepthMap2DBuilder(FrameworkImpl framework) {
        super(DepthMap2DBuilder.class, DepthMap2D.class, TextureImpl.Target.TEX_2D, framework);
    }

    @Override
    protected DepthMap2D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsDepthMap2D(handle);
    }

    @Override
    public ImageData<? extends DepthData> depth() {
        return singleDepth();
    }

    @Override
    public ImageData<? extends DepthStencilData> depthStencil() {
        return singleDepthStencil();
    }
}

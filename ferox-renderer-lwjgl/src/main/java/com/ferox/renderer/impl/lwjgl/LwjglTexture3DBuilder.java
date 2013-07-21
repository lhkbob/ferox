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

import com.ferox.renderer.Texture3D;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.Texture3DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture3DBuilder extends LwjglSamplerBuilder<Texture3D, Texture3DBuilder>
        implements Texture3DBuilder {
    public LwjglTexture3DBuilder(FrameworkImpl framework) {
        super(Texture3DBuilder.class, Texture3D.class, TextureImpl.Target.TEX_3D, framework);
        // preconfigure abstract builder for this type of texture
        imageCount(1);
    }

    @Override
    protected Texture3D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture3D(handle);
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> r() {
        return singleR();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> rg() {
        return singleRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture3D, RGBData> rgb() {
        return (SingleImageBuilder) singleRGB();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleImageBuilder<Texture3D, BasicColorData> rgba() {
        return (SingleImageBuilder) singleRGBA();
    }

    @Override
    public SingleImageBuilder<Texture3D, BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public SingleImageBuilder<Texture3D, ARGBData> argb() {
        return singleARGB();
    }
}

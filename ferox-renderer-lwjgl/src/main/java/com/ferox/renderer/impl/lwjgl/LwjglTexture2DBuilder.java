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

import com.ferox.renderer.Texture2D;
import com.ferox.renderer.builder.SingleImageBuilder;
import com.ferox.renderer.builder.Texture2DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture2DBuilder extends LwjglSamplerBuilder<Texture2D, Texture2DBuilder>
        implements Texture2DBuilder {
    public LwjglTexture2DBuilder(FrameworkImpl framework) {
        super(Texture2DBuilder.class, Texture2D.class, TextureImpl.Target.TEX_2D, framework);
        // preconfigure abstract builder for this type of texture
        depth(1);
        imageCount(1);
    }

    @Override
    protected Texture2D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture2D(handle);
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> r() {
        return singleR();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> rg() {
        return singleRG();
    }

    @Override
    public SingleImageBuilder<Texture2D, CompressedRGBData> rgb() {
        return singleRGB();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    public SingleImageBuilder<Texture2D, CompressedRGBAData> rgba() {
        return singleRGBA();
    }

    @Override
    public SingleImageBuilder<Texture2D, BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public SingleImageBuilder<Texture2D, ARGBData> argb() {
        return singleARGB();
    }
}

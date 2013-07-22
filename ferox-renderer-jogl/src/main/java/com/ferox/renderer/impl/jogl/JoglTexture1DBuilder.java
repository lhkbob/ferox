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

import com.ferox.renderer.Texture1D;
import com.ferox.renderer.builder.ImageData;
import com.ferox.renderer.builder.Texture1DBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class JoglTexture1DBuilder extends JoglSamplerBuilder<Texture1D, Texture1DBuilder>
        implements Texture1DBuilder {
    public JoglTexture1DBuilder(FrameworkImpl framework) {
        super(Texture1DBuilder.class, Texture1D.class, TextureImpl.Target.TEX_1D, framework);
        // preconfigure abstract builder for this type of texture
        height(1);
        depth(1);
        imageCount(1);
    }

    @Override
    protected Texture1D wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture1D(handle);
    }

    @Override
    public ImageData<? extends BasicColorData> r() {
        return singleR();
    }

    @Override
    public ImageData<? extends BasicColorData> rg() {
        return singleRG();
    }

    @Override
    public ImageData<? extends RGBData> rgb() {
        return singleRGB();
    }

    @Override
    public ImageData<? extends BasicColorData> bgr() {
        return singleBGR();
    }

    @Override
    public ImageData<? extends BasicColorData> rgba() {
        return singleRGBA();
    }

    @Override
    public ImageData<? extends BasicColorData> bgra() {
        return singleBGRA();
    }

    @Override
    public ImageData<? extends ARGBData> argb() {
        return singleARGB();
    }
}

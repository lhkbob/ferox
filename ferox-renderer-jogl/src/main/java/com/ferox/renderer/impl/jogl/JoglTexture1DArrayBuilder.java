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

import com.ferox.renderer.Texture1DArray;
import com.ferox.renderer.builder.ArrayImageData;
import com.ferox.renderer.builder.Texture1DArrayBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class JoglTexture1DArrayBuilder extends JoglSamplerBuilder<Texture1DArray, Texture1DArrayBuilder>
        implements Texture1DArrayBuilder {
    public JoglTexture1DArrayBuilder(FrameworkImpl framework) {
        super(Texture1DArrayBuilder.class, Texture1DArray.class, TextureImpl.Target.TEX_1D_ARRAY, framework);
        // preconfigure abstract builder for this type of texture
        height(1);
        depth(1);
    }

    @Override
    protected Texture1DArray wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture1DArray(handle);
    }

    @Override
    public ArrayImageData<? extends BasicColorData> r() {
        return arrayR();
    }

    @Override
    public ArrayImageData<? extends BasicColorData> rg() {
        return arrayRG();
    }

    @Override
    public ArrayImageData<? extends RGBData> rgb() {
        return arrayRGB();
    }

    @Override
    public ArrayImageData<? extends BasicColorData> bgr() {
        return arrayBGR();
    }

    @Override
    public ArrayImageData<? extends BasicColorData> rgba() {
        return arrayRGBA();
    }

    @Override
    public ArrayImageData<? extends BasicColorData> bgra() {
        return arrayBGRA();
    }

    @Override
    public ArrayImageData<? extends ARGBData> argb() {
        return arrayARGB();
    }
}

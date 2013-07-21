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

import com.ferox.renderer.Texture2DArray;
import com.ferox.renderer.builder.ArrayImageBuilder;
import com.ferox.renderer.builder.Texture2DArrayBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 *
 */
public class LwjglTexture2DArrayBuilder extends LwjglSamplerBuilder<Texture2DArray, Texture2DArrayBuilder>
        implements Texture2DArrayBuilder {
    public LwjglTexture2DArrayBuilder(FrameworkImpl framework) {
        super(Texture2DArrayBuilder.class, Texture2DArray.class, TextureImpl.Target.TEX_2D_ARRAY, framework);
        // preconfigure abstract builder for this type of texture
        depth(1);
    }

    @Override
    protected Texture2DArray wrap(TextureImpl.TextureHandle handle) {
        return wrapAsTexture2DArray(handle);
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> r() {
        return arrayR();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rg() {
        return arrayRG();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture2DArray, RGBData> rgb() {
        return (ArrayImageBuilder) arrayRGB();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgr() {
        return arrayBGR();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rgba() {
        return (ArrayImageBuilder) arrayRGBA();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgra() {
        return arrayBGRA();
    }

    @Override
    public ArrayImageBuilder<Texture2DArray, ARGBData> argb() {
        return arrayARGB();
    }
}

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
package com.ferox.renderer.impl;

import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.Surface;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

import java.util.Arrays;

public class SharedState {
    public static final Vector4 DEFAULT_BLEND_COLOR = new Vector4(0f, 0f, 0f, 0f);

    // blending
    public final Vector4 blendColor;
    public BlendFunction blendFuncRgb;
    public BlendFunction blendFuncAlpha;

    public BlendFactor blendSrcRgb;
    public BlendFactor blendDstRgb;
    public BlendFactor blendSrcAlpha;
    public BlendFactor blendDstAlpha;

    public boolean blendEnabled;

    // color masking [red, green, blue, alpha]
    public final boolean[] colorMask;

    // depth offsets
    public double depthOffsetFactor;
    public double depthOffsetUnits;
    public boolean depthOffsetEnabled;

    // depth test and mask
    public Comparison depthTest;
    public boolean depthMask;

    // draw styles
    public DrawStyle styleFront;
    public DrawStyle styleBack;

    // stencil test
    public Comparison stencilTestFront;
    public int stencilRefFront;
    public int stencilTestMaskFront;

    public StencilUpdate stencilFailFront;
    public StencilUpdate depthFailFront;
    public StencilUpdate depthPassFront;

    public Comparison stencilTestBack;
    public int stencilRefBack;
    public int stencilTestMaskBack;

    public StencilUpdate stencilFailBack;
    public StencilUpdate depthFailBack;
    public StencilUpdate depthPassBack;

    public boolean stencilEnabled;

    // bindable resources
    public BufferImpl.BufferHandle elementVBO;
    public BufferImpl.BufferHandle arrayVBO;

    public final TextureImpl.TextureHandle[] textures;
    public ShaderImpl.ShaderHandle shader;

    public int activeTexture;

    // stencil mask
    public int stencilMaskFront;
    public int stencilMaskBack;

    // viewport
    public int viewX;
    public int viewY;
    public int viewWidth;
    public int viewHeight;

    public SharedState(Surface surface) {
        blendColor = new Vector4(DEFAULT_BLEND_COLOR);
        blendFuncRgb = BlendFunction.ADD;
        blendFuncAlpha = BlendFunction.ADD;

        blendSrcRgb = BlendFactor.ONE;
        blendDstRgb = BlendFactor.ZERO;
        blendSrcAlpha = BlendFactor.ONE;
        blendDstAlpha = BlendFactor.ZERO;

        blendEnabled = false;

        colorMask = new boolean[] { true, true, true, true };

        depthOffsetFactor = 0;
        depthOffsetUnits = 0;
        depthOffsetEnabled = false;

        depthTest = Comparison.LESS;
        depthMask = true;

        styleFront = DrawStyle.SOLID;
        styleBack = DrawStyle.NONE;

        stencilTestFront = Comparison.ALWAYS;
        stencilRefFront = 0;
        stencilTestMaskFront = ~0;
        stencilFailFront = StencilUpdate.KEEP;
        depthFailFront = StencilUpdate.KEEP;
        depthPassFront = StencilUpdate.KEEP;

        stencilTestBack = Comparison.ALWAYS;
        stencilRefBack = 0;
        stencilTestMaskBack = ~0;
        stencilFailBack = StencilUpdate.KEEP;
        depthFailBack = StencilUpdate.KEEP;
        depthPassBack = StencilUpdate.KEEP;

        stencilEnabled = false;

        stencilMaskFront = ~0;
        stencilMaskBack = ~0;

        viewX = 0;
        viewY = 0;
        viewWidth = surface.getWidth();
        viewHeight = surface.getHeight();

        elementVBO = null;
        arrayVBO = null;
        shader = null;
        textures = new TextureImpl.TextureHandle[surface.getFramework().getCapabilities()
                                                        .getMaxCombinedTextures()];
        activeTexture = 0;
    }

    public SharedState(SharedState toClone) {
        blendColor = new Vector4(toClone.blendColor);
        blendFuncRgb = toClone.blendFuncRgb;
        blendFuncAlpha = toClone.blendFuncAlpha;

        blendSrcRgb = toClone.blendSrcRgb;
        blendDstRgb = toClone.blendDstRgb;
        blendSrcAlpha = toClone.blendSrcAlpha;
        blendDstAlpha = toClone.blendDstAlpha;

        blendEnabled = toClone.blendEnabled;

        colorMask = Arrays.copyOf(toClone.colorMask, toClone.colorMask.length);

        depthOffsetFactor = toClone.depthOffsetFactor;
        depthOffsetUnits = toClone.depthOffsetUnits;
        depthOffsetEnabled = toClone.depthOffsetEnabled;

        depthTest = toClone.depthTest;
        depthMask = toClone.depthMask;

        styleFront = toClone.styleFront;
        styleBack = toClone.styleBack;

        stencilTestFront = toClone.stencilTestFront;
        stencilRefFront = toClone.stencilRefFront;
        stencilTestMaskFront = toClone.stencilTestMaskFront;
        stencilFailFront = toClone.stencilFailFront;
        depthFailFront = toClone.depthFailBack;
        depthPassFront = toClone.depthPassFront;

        stencilTestBack = toClone.stencilTestBack;
        stencilRefBack = toClone.stencilRefBack;
        stencilTestMaskBack = toClone.stencilTestMaskBack;
        stencilFailBack = toClone.stencilFailBack;
        depthFailBack = toClone.depthFailBack;
        depthPassBack = toClone.depthPassBack;

        stencilEnabled = toClone.stencilEnabled;

        stencilMaskFront = toClone.stencilMaskFront;
        stencilMaskBack = toClone.stencilMaskBack;

        viewX = toClone.viewX;
        viewY = toClone.viewY;
        viewWidth = toClone.viewWidth;
        viewHeight = toClone.viewHeight;

        elementVBO = toClone.elementVBO;
        arrayVBO = toClone.arrayVBO;
        shader = toClone.shader;
        textures = Arrays.copyOf(toClone.textures, toClone.textures.length);
        activeTexture = toClone.activeTexture;
    }
}

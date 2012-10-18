package com.ferox.renderer.impl;

import java.util.Arrays;

import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.StencilUpdate;
import com.ferox.renderer.Surface;

public class RendererState {
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

    // stencil mask
    public int stencilMaskFront;
    public int stencilMaskBack;

    // viewport
    public int viewX;
    public int viewY;
    public int viewWidth;
    public int viewHeight;

    public RendererState(Surface surface) {
        blendColor = new Vector4(DEFAULT_BLEND_COLOR);
        blendFuncRgb = BlendFunction.ADD;
        blendFuncAlpha = BlendFunction.ADD;

        blendSrcRgb = BlendFactor.ONE;
        blendDstRgb = BlendFactor.ZERO;
        blendSrcAlpha = BlendFactor.ONE;
        blendDstAlpha = BlendFactor.ZERO;

        blendEnabled = false;

        colorMask = new boolean[] {true, true, true, true};

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
    }

    public RendererState(RendererState toClone) {
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
    }
}

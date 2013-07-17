package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.impl.FixedFunctionState;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.renderer.impl.ShaderOnlyState;

/**
 *
 */
public class LwjglRendererProvider implements RendererProvider {
    private final LwjglFixedFunctionRenderer fixed;
    private final LwjglGlslRenderer glsl;

    public LwjglRendererProvider(FixedFunctionState fixed, ShaderOnlyState glsl) {
        LwjglRendererDelegate shared = new LwjglRendererDelegate();
        if (fixed != null) {
            this.fixed = new LwjglFixedFunctionRenderer(shared);
        } else {
            this.fixed = null;
        }
        if (glsl != null) {
            this.glsl = new LwjglGlslRenderer(shared);
        } else {
            this.glsl = null;
        }
    }

    @Override
    public FixedFunctionRenderer getFixedFunctionRenderer(Capabilities caps) {
        return fixed;
    }

    @Override
    public GlslRenderer getGlslRenderer(Capabilities caps) {
        return glsl;
    }
}

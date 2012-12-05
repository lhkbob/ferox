package com.ferox.scene.controller.ffp;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.scene.AtmosphericFog.Falloff;

public class FogState implements State {
    // since exponential fog can't actually reach 0, we select a low value
    // to consider 'fully opaque' when solving for the density
    private static final double EXP_CUTOFF = .0001;

    private final Vector4 color;
    private final Falloff falloff;
    private final double density;
    private final double start;
    private final double end;

    public FogState(@Const ColorRGB color, Falloff falloff, double distanceToOpaque) {
        this.color = new Vector4(color.red(), color.green(), color.blue(), 1.0);
        this.falloff = falloff;

        switch (falloff) {
        case EXPONENTIAL:
            density = -Math.log(EXP_CUTOFF) / distanceToOpaque;
            start = 0;
            end = 0;
            break;
        case EXPONENTIAL_SQUARED:
            density = Math.sqrt(-Math.log(EXP_CUTOFF)) / distanceToOpaque;
            start = 0;
            end = 0;
            break;
        case LINEAR:
            start = 0;
            end = distanceToOpaque;
            density = 0;
            break;
        default:
            start = end = density = 0;
            break;
        }
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setFogEnabled(true);
        r.setFogColor(color);
        switch (falloff) {
        case EXPONENTIAL:
            r.setFogExponential(density, false);
            break;
        case EXPONENTIAL_SQUARED:
            r.setFogExponential(density, true);
            break;
        case LINEAR:
            r.setFogLinear(start, end);
            break;
        }

        currentNode.visitChildren(effects, access);

        // restore fog state so nodes without fog are not rendered incorrectly
        r.setFogEnabled(false);
    }
}

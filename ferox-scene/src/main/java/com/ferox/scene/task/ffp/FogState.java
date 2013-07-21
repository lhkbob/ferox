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
package com.ferox.scene.task.ffp;

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
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
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

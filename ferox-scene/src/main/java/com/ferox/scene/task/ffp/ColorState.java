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

public class ColorState implements State {
    public static final Vector4 DEFAULT_DIFFUSE = new Vector4(0.8, 0.8, 0.8, 1.0);
    public static final Vector4 DEFAULT_SPECULAR = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_EMITTED = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_AMBIENT = new Vector4(0.2, 0.2, 0.2, 1.0);

    private final Vector4 diffuse = new Vector4();
    private final Vector4 specular = new Vector4();
    private final Vector4 emitted = new Vector4();

    private double shininess;

    public void set(@Const ColorRGB diffuse, @Const ColorRGB specular, @Const ColorRGB emitted, double alpha,
                    double shininess) {
        if (diffuse == null) {
            this.diffuse.set(DEFAULT_DIFFUSE).w = alpha;
        } else {
            this.diffuse.set(diffuse.red(), diffuse.green(), diffuse.blue(), alpha);
        }

        if (specular == null) {
            this.specular.set(DEFAULT_SPECULAR).w = alpha;
        } else {
            this.specular.set(specular.red(), specular.green(), specular.blue(), alpha);
        }

        if (emitted == null) {
            this.emitted.set(DEFAULT_EMITTED).w = alpha;
        } else {
            this.emitted.set(emitted.red(), emitted.green(), emitted.blue(), alpha);
        }

        this.shininess = shininess;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setMaterial(DEFAULT_AMBIENT, diffuse, specular, emitted);
        r.setMaterialShininess(shininess);

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(shininess);

        int hash = 17 * (int) (bits ^ (bits >>> 32));
        hash = 31 * hash + diffuse.hashCode();
        hash = 31 * hash + emitted.hashCode();
        hash = 31 * hash + specular.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorState)) {
            return false;
        }

        ColorState ts = (ColorState) o;
        return ts.diffuse.equals(diffuse) && ts.emitted.equals(emitted) &&
               ts.specular.equals(specular) && shininess == shininess;
    }
}

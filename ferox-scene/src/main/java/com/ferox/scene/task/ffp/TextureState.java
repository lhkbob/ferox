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

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOperand;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.VertexAttribute;
import com.ferox.renderer.texture.Texture;

public class TextureState implements State {
    private final int diffuseTextureUnit;
    private final int decalTextureUnit;
    private final int emittedTextureUnit;

    private Texture diffuseTexture;
    private Texture decalTexture;
    private Texture emittedTexture;

    private VertexAttribute diffuseTexCoords;
    private VertexAttribute decalTexCoords;
    private VertexAttribute emittedTexCoords;

    public TextureState(int diffuseTextureUnit, int decalTextureUnit, int emittedTextureUnit) {
        this.diffuseTextureUnit = diffuseTextureUnit;
        this.decalTextureUnit = decalTextureUnit;
        this.emittedTextureUnit = emittedTextureUnit;
    }

    public void set(Texture diffuseTexture, VertexAttribute diffuseTexCoords, Texture decalTexture,
                    VertexAttribute decalTexCoords, Texture emittedTexture,
                    VertexAttribute emittedTexCoords) {
        this.diffuseTexture = diffuseTexture;
        this.decalTexture = decalTexture;
        this.emittedTexture = emittedTexture;

        this.diffuseTexCoords = diffuseTexCoords;
        this.decalTexCoords = decalTexCoords;
        this.emittedTexCoords = emittedTexCoords;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        if (diffuseTexture != null) {
            r.setTexture(diffuseTextureUnit, diffuseTexture);
            r.setTextureCoordinates(diffuseTextureUnit, diffuseTexCoords);
            // multiplicative blending with vertex color
            r.setTextureCombineRGB(diffuseTextureUnit, CombineFunction.MODULATE, CombineSource.CURR_TEX,
                                   CombineOperand.COLOR, CombineSource.PREV_TEX, CombineOperand.COLOR,
                                   CombineSource.CONST_COLOR, CombineOperand.COLOR);
            r.setTextureCombineAlpha(diffuseTextureUnit, CombineFunction.MODULATE, CombineSource.CURR_TEX,
                                     CombineOperand.ALPHA, CombineSource.PREV_TEX, CombineOperand.ALPHA,
                                     CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable diffuse texture
            r.setTexture(diffuseTextureUnit, null);
        }

        if (decalTexture != null) {
            r.setTexture(decalTextureUnit, decalTexture);
            r.setTextureCoordinates(decalTextureUnit, decalTexCoords);
            // alpha blended with previous color based on decal map alpha
            r.setTextureCombineRGB(decalTextureUnit, CombineFunction.INTERPOLATE, CombineSource.CURR_TEX,
                                   CombineOperand.COLOR, CombineSource.PREV_TEX, CombineOperand.COLOR,
                                   CombineSource.CURR_TEX, CombineOperand.ALPHA);
            // REPLACE with alpha(PREV_TEX) as arg0 preserves original alpha
            r.setTextureCombineAlpha(decalTextureUnit, CombineFunction.REPLACE, CombineSource.PREV_TEX,
                                     CombineOperand.ALPHA, CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                     CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable decal texture but only if we're on a different unit
            if (diffuseTextureUnit != decalTextureUnit) {
                r.setTexture(decalTextureUnit, null);
            }
        }

        if (emittedTexture != null) {
            r.setTexture(emittedTextureUnit, emittedTexture);
            r.setTextureCoordinates(emittedTextureUnit, emittedTexCoords);
            // emitted light is just added to the color output
            r.setTextureCombineRGB(emittedTextureUnit, CombineFunction.ADD, CombineSource.CURR_TEX,
                                   CombineOperand.COLOR, CombineSource.PREV_TEX, CombineOperand.COLOR,
                                   CombineSource.CONST_COLOR, CombineOperand.COLOR);
            // REPLACE with alpha(PREV_TEX) as arg0 preserves the original
            // alpha
            r.setTextureCombineAlpha(emittedTextureUnit, CombineFunction.REPLACE, CombineSource.PREV_TEX,
                                     CombineOperand.ALPHA, CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                     CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable emissive texture, but only if we're on a different
            // unit (to prevent overwrite)
            if (diffuseTextureUnit != emittedTextureUnit && decalTextureUnit != emittedTextureUnit) {
                r.setTexture(emittedTextureUnit, null);
            }
        }

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (diffuseTexture == null ? 0 : diffuseTexture.hashCode());
        hash = 31 * hash + (decalTexture == null ? 0 : decalTexture.hashCode());
        hash = 31 * hash + (emittedTexture == null ? 0 : emittedTexture.hashCode());
        hash = 31 * hash + (diffuseTexCoords == null ? 0 : diffuseTexCoords.hashCode());
        hash = 31 * hash + (decalTexCoords == null ? 0 : decalTexCoords.hashCode());
        hash = 31 * hash + (emittedTexCoords == null ? 0 : emittedTexCoords.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextureState)) {
            return false;
        }

        TextureState ts = (TextureState) o;
        boolean diff = nullEquals(ts.diffuseTexture, diffuseTexture) &&
                       nullEquals(ts.diffuseTexCoords, diffuseTexCoords);
        boolean decal =
                nullEquals(ts.decalTexture, decalTexture) && nullEquals(ts.decalTexCoords, decalTexCoords);
        boolean emit = nullEquals(ts.emittedTexture, emittedTexture) &&
                       nullEquals(ts.emittedTexCoords, emittedTexCoords);
        return diff && decal && emit;
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }
}

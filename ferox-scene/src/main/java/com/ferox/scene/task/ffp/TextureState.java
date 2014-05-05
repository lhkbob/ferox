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
import com.ferox.renderer.Texture;

public class TextureState implements State {
    private Texture diffuseTexture;
    private Texture decalTexture;
    private Texture emittedTexture;

    public void set(Texture diffuseTexture, Texture decalTexture, Texture emittedTexture) {
        this.diffuseTexture = diffuseTexture;
        this.decalTexture = decalTexture;
        this.emittedTexture = emittedTexture;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        if (diffuseTexture != null) {
            r.setTexture(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, diffuseTexture);
            // multiplicative blending with vertex color
            r.setTextureCombineRGB(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, CombineFunction.MODULATE,
                                   CombineSource.CURR_TEX, CombineOperand.COLOR, CombineSource.PREV_TEX,
                                   CombineOperand.COLOR, CombineSource.CONST_COLOR, CombineOperand.COLOR);
            r.setTextureCombineAlpha(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, CombineFunction.MODULATE,
                                     CombineSource.CURR_TEX, CombineOperand.ALPHA, CombineSource.PREV_TEX,
                                     CombineOperand.ALPHA, CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable diffuse texture
            r.setTexture(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, null);
        }

        if (decalTexture != null) {
            r.setTexture(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, decalTexture);
            // alpha blended with previous color based on decal map alpha
            r.setTextureCombineRGB(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, CombineFunction.INTERPOLATE,
                                   CombineSource.CURR_TEX, CombineOperand.COLOR, CombineSource.PREV_TEX,
                                   CombineOperand.COLOR, CombineSource.CURR_TEX, CombineOperand.ALPHA);
            // REPLACE with alpha(PREV_TEX) as arg0 preserves original alpha
            r.setTextureCombineAlpha(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, CombineFunction.REPLACE,
                                     CombineSource.PREV_TEX, CombineOperand.ALPHA, CombineSource.CURR_TEX,
                                     CombineOperand.ALPHA, CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable decal texture
            r.setTexture(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, null);
        }

        if (emittedTexture != null) {
            r.setTexture(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, emittedTexture);
            // emitted light is just added to the color output
            r.setTextureCombineRGB(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, CombineFunction.ADD,
                                   CombineSource.CURR_TEX, CombineOperand.COLOR, CombineSource.PREV_TEX,
                                   CombineOperand.COLOR, CombineSource.CONST_COLOR, CombineOperand.COLOR);
            // REPLACE with alpha(PREV_TEX) as arg0 preserves the original alpha
            r.setTextureCombineAlpha(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, CombineFunction.REPLACE,
                                     CombineSource.PREV_TEX, CombineOperand.ALPHA, CombineSource.CURR_TEX,
                                     CombineOperand.ALPHA, CombineSource.CONST_COLOR, CombineOperand.ALPHA);
        } else {
            // disable emissive texture
            r.setTexture(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, null);
        }

        currentNode.visitChildren(effects.applyTexturing(diffuseTexture != null, decalTexture != null,
                                                         emittedTexture != null), access);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (diffuseTexture == null ? 0 : diffuseTexture.hashCode());
        hash = 31 * hash + (decalTexture == null ? 0 : decalTexture.hashCode());
        hash = 31 * hash + (emittedTexture == null ? 0 : emittedTexture.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextureState)) {
            return false;
        }

        TextureState ts = (TextureState) o;
        return nullEquals(ts.diffuseTexture, diffuseTexture) && nullEquals(ts.decalTexture, decalTexture) &&
               nullEquals(ts.emittedTexture, emittedTexture);
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }
}

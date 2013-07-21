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

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;

public class TransparentState implements State {
    public static final TransparentState OPAQUE = new TransparentState(BlendFactor.ONE, BlendFactor.ZERO);
    public static final TransparentState NORMAL = new TransparentState(BlendFactor.SRC_ALPHA,
                                                                       BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final TransparentState ADDITIVE = new TransparentState(BlendFactor.SRC_ALPHA,
                                                                         BlendFactor.ONE);

    private final BlendFactor src;
    private final BlendFactor dst;

    public TransparentState(BlendFactor src, BlendFactor dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        AppliedEffects newEffects = effects.applyBlending(src, dst);

        newEffects.pushBlending(access.getCurrentContext().getFixedFunctionRenderer());
        access.getCurrentContext().getFixedFunctionRenderer()
              .setTwoSidedLightingEnabled(newEffects.isBlendingEnabled());
        currentNode.visitChildren(newEffects, access);
    }
}

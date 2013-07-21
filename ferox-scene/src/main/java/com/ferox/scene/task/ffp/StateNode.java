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

import java.util.Arrays;

public class StateNode {
    private final State state;
    private StateNode[] children;

    private int maxIndex;

    public StateNode(State state) {
        this.state = state;
        children = null;
        maxIndex = 0;
    }

    public StateNode(State state, int expectedChildCount) {
        this.state = state;
        children = new StateNode[expectedChildCount];
    }

    public State getState() {
        return state;
    }

    public void setChild(int index, StateNode child) {
        if (children == null) {
            children = new StateNode[2 * (index + 1)];
        } else if (children.length <= index) {
            children = Arrays.copyOf(children, 2 * index);
        }

        children[index] = child;
        maxIndex = Math.max(index + 1, maxIndex);
    }

    public void addChild(StateNode child) {
        setChild(maxIndex, child);
    }

    public StateNode getChild(int index) {
        if (children == null || children.length <= index) {
            return null;
        }
        return children[index];
    }

    public void visit(AppliedEffects effects, HardwareAccessLayer access) {
        state.visitNode(this, effects, access);
    }

    public void visitChildren(AppliedEffects effects, HardwareAccessLayer access) {
        if (children != null) {
            for (int i = 0; i < maxIndex; i++) {
                if (children[i] != null) {
                    children[i].visit(effects, access);
                }
            }
        }
    }
}

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

import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.resources.BufferImpl;

import java.util.Arrays;

/**
 *
 */
public class ShaderOnlyState {
    public static class AttributeState {
        public final int index;

        // non-null means attribute is handled by a vbo pointer
        public BufferImpl.BufferHandle vbo;

        public int offset;
        public int stride;
        public int elementSize;

        // otherwise attribute data comes from these values
        public final float[] floatAttrValues;
        public final int[] intAttrValues;

        // one of INT, UNSIGNED_INT, or FLOAT, or null when vbo is not null
        public DataType dataType;

        public AttributeState(int index) {
            this.index = index;

            vbo = null;
            offset = 0;
            stride = 0;
            elementSize = 0;

            floatAttrValues = new float[4];
            intAttrValues = new int[4];
            dataType = DataType.FLOAT;
        }

        public AttributeState(AttributeState state) {
            index = state.index;
            vbo = state.vbo;
            offset = state.offset;
            stride = state.stride;
            elementSize = state.elementSize;

            floatAttrValues = Arrays.copyOf(state.floatAttrValues, state.floatAttrValues.length);
            intAttrValues = Arrays.copyOf(state.intAttrValues, state.intAttrValues.length);

            dataType = state.dataType;
        }
    }

    public final AttributeState[] attributes;

    public ShaderOnlyState(int numAttributes) {
        attributes = new AttributeState[numAttributes];
        for (int i = 0; i < numAttributes; i++) {
            attributes[i] = new AttributeState(i);
        }
    }

    public ShaderOnlyState(ShaderOnlyState toClone) {
        attributes = new AttributeState[toClone.attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = new AttributeState(toClone.attributes[i]);
        }
    }
}

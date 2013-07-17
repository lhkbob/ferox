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
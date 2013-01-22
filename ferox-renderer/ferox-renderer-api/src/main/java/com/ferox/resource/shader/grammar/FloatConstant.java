package com.ferox.resource.shader.grammar;

public class FloatConstant implements PrimaryExpression {
    private final float value;

    public FloatConstant(float value) {
        this.value = value;
    }
}

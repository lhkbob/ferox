package com.ferox.resource.shader.grammar;

public class IntConstant implements PrimaryExpression {
    private final int value;

    public IntConstant(int value) {
        this.value = value;
    }
}

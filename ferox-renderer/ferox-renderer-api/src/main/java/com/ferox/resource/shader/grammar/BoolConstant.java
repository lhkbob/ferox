package com.ferox.resource.shader.grammar;

public class BoolConstant implements PrimaryExpression {
    private final boolean value;

    public BoolConstant(boolean value) {
        this.value = value;
    }
}

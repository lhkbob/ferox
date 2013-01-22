package com.ferox.resource.shader.grammar;

public class PrefixDecrement implements UnaryExpression {
    private final UnaryExpression expression;

    public PrefixDecrement(UnaryExpression expression) {
        this.expression = expression;
    }
}

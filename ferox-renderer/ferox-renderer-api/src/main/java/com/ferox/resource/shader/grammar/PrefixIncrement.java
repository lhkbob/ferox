package com.ferox.resource.shader.grammar;

public class PrefixIncrement implements UnaryExpression {
    private final UnaryExpression expression;

    public PrefixIncrement(UnaryExpression expression) {
        this.expression = expression;
    }
}

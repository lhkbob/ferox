package com.ferox.resource.shader.grammar;

public class NestedExpression implements PrimaryExpression {
    private final Expression expression;

    public NestedExpression(Expression expression) {
        this.expression = expression;
    }
}

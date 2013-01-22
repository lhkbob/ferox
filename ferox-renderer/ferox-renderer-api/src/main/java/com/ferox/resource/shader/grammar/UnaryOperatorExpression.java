package com.ferox.resource.shader.grammar;

public class UnaryOperatorExpression implements UnaryExpression {
    public static enum UnaryOperator {
        PLUS,
        DASH,
        BANG,
        TILDE /* reserved */
    }

    private final UnaryOperator operator;
    private final UnaryExpression expression;

    public UnaryOperatorExpression(UnaryOperator operator, UnaryExpression expression) {
        this.operator = operator;
        this.expression = expression;
    }
}

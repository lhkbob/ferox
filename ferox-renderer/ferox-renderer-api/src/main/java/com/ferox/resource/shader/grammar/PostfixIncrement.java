package com.ferox.resource.shader.grammar;

public class PostfixIncrement implements PostfixExpression {
    private final PostfixExpression expression;

    public PostfixIncrement(PostfixExpression expression) {
        this.expression = expression;
    }
}

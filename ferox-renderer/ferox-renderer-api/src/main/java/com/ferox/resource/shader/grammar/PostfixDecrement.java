package com.ferox.resource.shader.grammar;

public class PostfixDecrement implements PostfixExpression {
    private final PostfixExpression expression;

    public PostfixDecrement(PostfixExpression expression) {
        this.expression = expression;
    }
}

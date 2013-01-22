package com.ferox.resource.shader.grammar;

public class ReturnStatement implements JumpStatement {
    private final Expression expression; // nullable for a void return

    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }
}

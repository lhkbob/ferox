package com.ferox.resource.shader.simple_grammar;

public class DoWhileLoop implements Statement {
    private final Statement body;
    private final Expression condition;

    public DoWhileLoop(Statement body, Expression condition) {
        this.condition = condition;
        this.body = body;
    }
}

package com.ferox.resource.shader.simple_grammar;

public class WhileLoop implements Statement {
    private final WhileLoopCondition condition;
    private final Statement body;

    public WhileLoop(WhileLoopCondition condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }
}

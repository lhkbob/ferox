package com.ferox.resource.shader.grammar;

public class WhileStatement implements IterationStatement {
    private final WhileLoopCondition condition;
    private final Statement body;

    public WhileStatement(WhileLoopCondition condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }
}

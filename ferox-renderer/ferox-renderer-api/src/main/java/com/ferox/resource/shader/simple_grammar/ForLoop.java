package com.ferox.resource.shader.simple_grammar;

public class ForLoop implements Statement {
    private final ForLoopInitializer initStatement;
    private final WhileLoopCondition condition; // nullable for no condition
    private final Expression increment; // nullable for no increment
    private final Statement body;

    public ForLoop(ForLoopInitializer initStatement, WhileLoopCondition condition,
                   Expression increment, Statement body) {
        this.initStatement = initStatement;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }
}

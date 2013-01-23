package com.ferox.resource.shader.simple_grammar;

public class IfThenElse implements Statement {
    private final Expression condition;
    private final Statement onTrue;
    private final Statement onFalse; // null for no else branch

    public IfThenElse(Expression condition, Statement onTrue, Statement onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }
}

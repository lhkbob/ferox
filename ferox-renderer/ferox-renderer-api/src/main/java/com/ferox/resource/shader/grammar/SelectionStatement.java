package com.ferox.resource.shader.grammar;

public class SelectionStatement implements SimpleStatement {
    private final Expression condition;
    private final Statement onTrue;
    private final Statement onFalse; // null for no else branch

    public SelectionStatement(Expression condition, Statement onTrue, Statement onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }
}

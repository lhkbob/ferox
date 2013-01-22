package com.ferox.resource.shader.grammar;

public class CompoundStatement implements Statement {
    private final Statement[] statements;

    public CompoundStatement(Statement... statements) {
        this.statements = statements;
    }
}

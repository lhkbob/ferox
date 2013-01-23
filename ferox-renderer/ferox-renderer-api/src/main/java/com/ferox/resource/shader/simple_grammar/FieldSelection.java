package com.ferox.resource.shader.simple_grammar;

public class FieldSelection implements Expression {
    private final Expression variable;
    private final String field;

    public FieldSelection(Expression variable, String field) {
        this.variable = variable;
        this.field = field;
    }
}

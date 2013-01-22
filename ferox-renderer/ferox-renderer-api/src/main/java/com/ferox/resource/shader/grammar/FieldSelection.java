package com.ferox.resource.shader.grammar;

public class FieldSelection implements PostfixExpression {
    private final PostfixExpression variable;
    private final String field;

    public FieldSelection(PostfixExpression variable, String field) {
        this.variable = variable;
        this.field = field;
    }
}

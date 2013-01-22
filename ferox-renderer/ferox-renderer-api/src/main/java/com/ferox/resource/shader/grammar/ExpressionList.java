package com.ferox.resource.shader.grammar;

public class ExpressionList implements Expression {
    private final Expression first;
    private final AssignmentExpression afterComma;

    public ExpressionList(Expression first, AssignmentExpression afterComma) {
        this.first = first;
        this.afterComma = afterComma;
    }
}

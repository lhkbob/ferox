package com.ferox.resource.shader.grammar;

public class ArrayAccess implements PostfixExpression {
    private final PostfixExpression array;
    private final Expression index; // int_expression

    public ArrayAccess(PostfixExpression array, Expression index) {
        this.array = array;
        this.index = index;
    }
}

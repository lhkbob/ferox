package com.ferox.resource.shader.simple_grammar;

public class ArrayAccess implements Expression {
    private final Expression array;
    private final Expression index; // int_expression

    public ArrayAccess(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }
}

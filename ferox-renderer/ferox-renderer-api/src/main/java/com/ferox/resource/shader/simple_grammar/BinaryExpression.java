package com.ferox.resource.shader.simple_grammar;

public class BinaryExpression implements Expression, LeftAssociative {
    public static enum BinaryOperator {
        MULTIPLY,
        DIVIDE,
        MODULO,
        ADD,
        SUBTRACT,
        LEFT_SHIFT,
        RIGHT_SHIFT,
        LESS_THAN,
        GREATER_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN_OR_EQUAL,
        EQUAL,
        NOT_EQUAL,
        LOGICAL_AND,
        LOGICAL_XOR,
        LOGICAL_OR
        // FIXME insert bitwise AND, XOR, and OR
    }

    private final Expression left;
    private final BinaryOperator operator;
    private final Expression right;

    public BinaryExpression(Expression left, BinaryOperator operator, Expression right) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
}

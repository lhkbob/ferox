package com.ferox.resource.shader.grammar;

public class BinaryOperatorExpression implements BinaryExpression {
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

    private final BinaryExpression left;
    private final BinaryOperator operator;
    private final UnaryExpression right;

    public BinaryOperatorExpression(BinaryExpression left, BinaryOperator operator,
                                    UnaryExpression right) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
}

package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class BinaryExpression extends AbstractExpression implements LeftAssociative {
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

    @Override
    public Type getType(Environment env) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Environment validate(Environment environment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}

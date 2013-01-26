package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.LValue;
import com.ferox.resource.shader.simple_grammar.BinaryExpression.BinaryOperator;
import com.ferox.resource.shader.simple_grammar.UnaryExpression.UnaryOperator;

public abstract class AbstractExpression implements Expression {

    @Override
    public Expression mul(Expression right) {
        return new BinaryExpression(this, BinaryOperator.MULTIPLY, right);
    }

    @Override
    public Expression add(Expression right) {
        return new BinaryExpression(this, BinaryOperator.ADD, right);
    }

    @Override
    public Expression sub(Expression right) {
        return new BinaryExpression(this, BinaryOperator.SUBTRACT, right);
    }

    @Override
    public Expression div(Expression right) {
        return new BinaryExpression(this, BinaryOperator.DIVIDE, right);
    }

    @Override
    public Expression lessThan(Expression right) {
        return new BinaryExpression(this, BinaryOperator.LESS_THAN, right);
    }

    @Override
    public Expression greaterThan(Expression right) {
        return new BinaryExpression(this, BinaryOperator.GREATER_THAN, right);
    }

    @Override
    public Expression le(Expression right) {
        return new BinaryExpression(this, BinaryOperator.LESS_THAN_OR_EQUAL, right);
    }

    @Override
    public Expression ge(Expression right) {
        return new BinaryExpression(this, BinaryOperator.GREATER_THAN_OR_EQUAL, right);
    }

    @Override
    public Expression equals(Expression right) {
        return new BinaryExpression(this, BinaryOperator.EQUAL, right);
    }

    @Override
    public Expression notEquals(Expression right) {
        return new BinaryExpression(this, BinaryOperator.NOT_EQUAL, right);
    }

    @Override
    public Expression logicalAnd(Expression right) {
        return new BinaryExpression(this, BinaryOperator.LOGICAL_AND, right);
    }

    @Override
    public Expression logicalOr(Expression right) {
        return new BinaryExpression(this, BinaryOperator.LOGICAL_OR, right);
    }

    @Override
    public Expression logicalXor(Expression right) {
        return new BinaryExpression(this, BinaryOperator.LOGICAL_XOR, right);
    }

    @Override
    public Expression negate() {
        return new UnaryExpression(UnaryOperator.DASH, this);
    }

    @Override
    public Expression not() {
        return new UnaryExpression(UnaryOperator.BANG, this);
    }

    @Override
    public Expression increment() {
        return new UnaryExpression(UnaryOperator.POSTFIX_INCREMENT, this);
    }

    @Override
    public Expression decrement() {
        return new UnaryExpression(UnaryOperator.POSTFIX_DECREMENT, this);
    }

    @Override
    public LValue field(String name) {
        return new FieldSelection(this, name);
    }

    @Override
    public LValue array(Expression index) {
        return new ArrayAccess(this, index);
    }
}

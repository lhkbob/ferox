package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.LValue;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.simple_grammar.BinaryExpression.BinaryOperator;
import com.ferox.resource.shader.simple_grammar.UnaryExpression.UnaryOperator;

public abstract class AbstractExpression implements Expression {
    public static enum Precedence {
        ASSIGNMENT_EXPRESSIONS,
        LOGICAL_OR_EXPRESSIONS,
        LOGICAL_XOR_EXPRESSIONS,
        LOGICAL_AND_EXPRESSIONS,

        // bitwise or (single |)
        OR_EXPRESSIONS,

        // bitwise xor (single ^)
        XOR_EXPRESSIONS,

        // bitwise and (single &)
        AND_EXPRESSIONS,

        // equal, not equal
        EQUALITY_EXPERSSIONS,

        // greater/less than (or equal)
        RELATIONAL_EXPRESSIONS,

        // left and right shifts
        SHIFT_EXPRESSIONS,

        // addition, subtraction
        ADDITIVE_EXPRESSIONS,

        // modulation, division, multiplication
        MULTIPLICATIVE_EXPRESSIONS,

        // bit inversions, logical and mathematical negation, prefix
        // increment/decrement
        UNARY_EXPRESSIONS,

        // postfix decrement/increment, field selection, array access,
        // and function calls
        POSTFIX_EXPRESSIONS,

        // variables, constants, nested expressions
        PRIMARY_EXPRESSIONS
    }

    @Override
    public LValue x() {
        return field("x");
    }

    @Override
    public LValue y() {
        return field("y");
    }

    @Override
    public LValue z() {
        return field("z");
    }

    @Override
    public LValue xy() {
        return field("xy");
    }

    @Override
    public LValue xyz() {
        return field("xyz");
    }

    @Override
    public LValue m(int col, int row) {
        return array(new Constant(col)).array(new Constant(row));
    }

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
        return new UnaryExpression(UnaryOperator.NEGATE, this);
    }

    @Override
    public Expression not() {
        return new UnaryExpression(UnaryOperator.LOGICAL_NEGATE, this);
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

    @Override
    public void emit(ShaderAccumulator shader) {
        shader.addLine(emitExpression(shader) + ";");
    }

    @Override
    public boolean containsDeclaration() {
        return false;
    }
}

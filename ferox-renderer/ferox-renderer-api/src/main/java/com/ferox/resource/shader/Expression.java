package com.ferox.resource.shader;

public interface Expression extends Statement {
    public Expression mul(Expression right);

    public Expression add(Expression right);

    public Expression sub(Expression right);

    public Expression div(Expression right);

    public Expression lessThan(Expression right);

    public Expression greaterThan(Expression right);

    public Expression le(Expression right);

    public Expression ge(Expression right);

    public Expression equals(Expression right);

    public Expression notEquals(Expression right);

    public Expression logicalAnd(Expression right);

    public Expression logicalOr(Expression right);

    public Expression logicalXor(Expression right);

    public Expression negate();

    public Expression not();

    public Expression increment();

    public Expression decrement();

    public LValue field(String name);

    public LValue array(Expression index);

    public Type getType(Environment env);
}

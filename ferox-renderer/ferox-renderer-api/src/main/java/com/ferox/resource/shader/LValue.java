package com.ferox.resource.shader;

public interface LValue extends Expression {
    public Expression setTo(Expression value);
}

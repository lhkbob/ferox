package com.ferox.resource.shader;

public interface LValue extends Expression {
    public Statement setTo(Expression value);
}

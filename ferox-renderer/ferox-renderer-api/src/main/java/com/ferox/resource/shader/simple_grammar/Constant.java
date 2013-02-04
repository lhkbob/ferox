package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.Type;

public class Constant extends AbstractExpression {
    private final Object value;
    private final Type type;

    public Constant(int value) {
        this.value = Integer.valueOf(value);
        this.type = PrimitiveType.INT;
    }

    public Constant(float value) {
        this.value = Float.valueOf(value);
        this.type = PrimitiveType.FLOAT;
    }

    public Constant(boolean value) {
        this.value = Boolean.valueOf(value);
        this.type = PrimitiveType.BOOL;
    }

    @Override
    public Type getType(Environment env) {
        return type;
    }

    @Override
    public Environment validate(Environment environment) {
        // constants are always valid
        return environment;
    }

    @Override
    public String emitExpression() {
        return value.toString();
    }

    @Override
    public int getPrecedence() {
        return Precedence.PRIMARY_EXPRESSIONS.ordinal();
    }
}

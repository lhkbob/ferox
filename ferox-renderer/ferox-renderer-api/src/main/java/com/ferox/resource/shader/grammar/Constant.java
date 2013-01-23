package com.ferox.resource.shader.grammar;

public class Constant implements PrimaryExpression {
    private final Object value;

    public Constant(int value) {
        this.value = Integer.valueOf(value);
    }

    public Constant(float value) {
        this.value = Float.valueOf(value);
    }

    public Constant(boolean value) {
        this.value = Boolean.valueOf(value);
    }
}

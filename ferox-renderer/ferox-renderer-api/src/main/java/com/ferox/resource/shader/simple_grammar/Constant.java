package com.ferox.resource.shader.simple_grammar;

public class Constant implements Expression {
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

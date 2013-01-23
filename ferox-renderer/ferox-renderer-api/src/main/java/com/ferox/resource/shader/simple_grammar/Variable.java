package com.ferox.resource.shader.simple_grammar;

public class Variable implements Expression {
    private final String identifier;

    public Variable(String identifier) {
        this.identifier = identifier;
    }
}

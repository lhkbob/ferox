package com.ferox.resource.shader.simple_grammar;

public class FunctionCall implements Expression {
    // this is a reasonable consolidation of the grammar for
    // function calls with and without parameters
    private final String identifier; // includes constructor calls for built-in types, etc
    private final Expression[] parameters;

    public FunctionCall(String identifier, Expression... parameters) {
        this.identifier = identifier;
        this.parameters = parameters;
    }
}

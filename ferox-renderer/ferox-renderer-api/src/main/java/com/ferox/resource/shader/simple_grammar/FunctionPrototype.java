package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.grammar.ParameterDeclaration;

public class FunctionPrototype implements Declaration {
    private final Type type;
    private final ParameterDeclaration[] parameters;

    public FunctionPrototype(Type type, ParameterDeclaration... parameters) {
        this.type = type;
        this.parameters = parameters;
    }
}

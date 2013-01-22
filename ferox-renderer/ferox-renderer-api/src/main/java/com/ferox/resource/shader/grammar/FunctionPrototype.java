package com.ferox.resource.shader.grammar;

public class FunctionPrototype implements Declaration {
    private final FullySpecifiedType type;
    private final ParameterDeclaration[] parameters;

    public FunctionPrototype(FullySpecifiedType type, ParameterDeclaration... parameters) {
        this.type = type;
        this.parameters = parameters;
    }
}

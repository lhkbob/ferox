package com.ferox.resource.shader.simple_grammar;

public class FunctionDefinition implements ExternalDeclaration {
    private final FunctionPrototype prototype;
    private final CompoundStatement body;

    public FunctionDefinition(FunctionPrototype prototype, CompoundStatement body) {
        this.prototype = prototype;
        this.body = body;
    }
}

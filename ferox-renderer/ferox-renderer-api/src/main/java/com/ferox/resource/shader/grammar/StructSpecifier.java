package com.ferox.resource.shader.grammar;

public class StructSpecifier implements TypeSpecifier {
    private final String identifier;
    private final StructDeclaration[] definition;

    public StructSpecifier(String identifier, StructDeclaration... definition) {
        this.identifier = identifier;
        this.definition = definition;
    }
}

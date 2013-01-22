package com.ferox.resource.shader.grammar;

public class StructSpecifier implements TypeSpecifier {
    private final StructDeclaration[] definition;

    public StructSpecifier(StructDeclaration... definition) {
        this.definition = definition;
    }
}

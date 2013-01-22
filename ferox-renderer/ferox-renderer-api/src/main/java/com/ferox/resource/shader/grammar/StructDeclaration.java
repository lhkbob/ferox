package com.ferox.resource.shader.grammar;

public class StructDeclaration {
    private final TypeSpecifier type;
    private final StructDeclarator[] identifiers;

    public StructDeclaration(TypeSpecifier type, StructDeclarator... identifiers) {
        this.type = type;
        this.identifiers = identifiers;
    }
}

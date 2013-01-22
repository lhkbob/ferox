package com.ferox.resource.shader.grammar;

public class FullySpecifiedType implements SingleDeclaration {
    public static enum TypeQualifier {
        CONST,
        // FIXME attribute and varying are deprecated and replaced by IN/OUT/INOUT
        ATTRIBUTE,
        VARYING,
        // FIXME are uniforms really allowed in a function parameter list?
        UNIFORM
    }

    private final TypeSpecifier type;
    private final TypeQualifier qualifier; // nullable for unqualified type

    public FullySpecifiedType(TypeQualifier qualifier, TypeSpecifier type) {
        this.type = type;
        this.qualifier = qualifier;
    }
}

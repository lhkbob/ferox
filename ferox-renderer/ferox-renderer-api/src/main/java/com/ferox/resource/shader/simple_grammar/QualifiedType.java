package com.ferox.resource.shader.simple_grammar;

public class QualifiedType implements Type {
    public static enum TypeQualifier {
        UNIFORM,
        IN, // attribute/varying
        OUT, // varying
        CONST,
    }

    private final TypeQualifier qualifier;
    private final Type type;

    public QualifiedType(TypeQualifier qualifier, Type type) {
        if (type instanceof QualifiedType) {
            throw new IllegalArgumentException("Cannot qualify a qualified type");
        }
        this.qualifier = qualifier;
        this.type = type;
    }
}

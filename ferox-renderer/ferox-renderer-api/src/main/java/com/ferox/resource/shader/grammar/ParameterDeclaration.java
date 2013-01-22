package com.ferox.resource.shader.grammar;

import com.ferox.resource.shader.grammar.FullySpecifiedType.TypeQualifier;

public class ParameterDeclaration {
    public static enum ParameterQualifier {
        IN,
        OUT,
        INOUT
    }

    // FIXME why doesn't this use a FullySpecifiedType?
    private final TypeQualifier typeQualifier; // nullable for no qualifier
    private final ParameterQualifier paramQualifier;

    private final TypeSpecifier type;
    private final String identifier; // nullable for unlabeled parameter
    private final ConstantExpression bracketExpression; // nullable for non-array type

    public ParameterDeclaration(TypeQualifier typeQualifier,
                                ParameterQualifier paramQualifier, TypeSpecifier type,
                                String identifier, ConstantExpression bracketExpression) {
        this.typeQualifier = typeQualifier;
        this.paramQualifier = paramQualifier;
        this.type = type;
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}

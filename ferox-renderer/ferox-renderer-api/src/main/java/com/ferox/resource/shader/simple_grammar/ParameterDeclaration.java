package com.ferox.resource.shader.simple_grammar;

public class ParameterDeclaration {
    public static enum ParameterQualifier {
        IN,
        OUT,
        INOUT
    }

    private final ParameterQualifier paramQualifier;

    private final Parameter parameter;

    public ParameterDeclaration(ParameterQualifier paramQualifier, Parameter parameter) {
        this.paramQualifier = paramQualifier;
        this.parameter = parameter;
    }
}

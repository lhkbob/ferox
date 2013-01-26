package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Type;

public class Parameter {
    public static enum ParameterQualifier {
        IN,
        OUT,
        INOUT,
        NONE
    }

    private final Type type;
    private final String name;
    private final ParameterQualifier qualifier;

    public Parameter(ParameterQualifier qualifier, Type type, String name) {
        this.qualifier = qualifier;
        this.type = type;
        this.name = name;
    }

    public ParameterQualifier getQualifier() {
        return qualifier;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}

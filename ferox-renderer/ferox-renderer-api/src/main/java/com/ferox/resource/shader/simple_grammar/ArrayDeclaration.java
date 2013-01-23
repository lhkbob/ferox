package com.ferox.resource.shader.simple_grammar;

public class ArrayDeclaration implements Declaration {
    //    private final TypeQualifier qualifier; FIXME
    private final Type type;
    private final String identifier;
    private final Expression bracketExpression; // must nest for >= assignment expression

    public ArrayDeclaration(Type type, String identifier,
                                      Expression bracketExpression) {
        this.type = type;
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}

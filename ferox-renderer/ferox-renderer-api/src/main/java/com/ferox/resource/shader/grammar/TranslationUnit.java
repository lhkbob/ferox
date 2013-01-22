package com.ferox.resource.shader.grammar;

public class TranslationUnit {
    private final ExternalDeclaration[] declarations;

    public TranslationUnit(ExternalDeclaration... declarations) {
        this.declarations = declarations;
    }
}

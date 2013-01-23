package com.ferox.resource.shader.simple_grammar;

public class Assignment implements Expression, RightAssociative {
    public static enum AssignmentOperator {
        EQUAL,
        MUL_ASSIGN,
        DIV_ASSIGN,
        MOD_ASSIGN,
        ADD_ASSIGN,
        SUB_ASSIGN
        // FIXME insert logical and bitwise assignment operators
    }

    private final Expression lvalue;
    private final AssignmentOperator operator;
    private final Expression rvalue;

    public Assignment(Expression lvalue, AssignmentOperator operator,
                                        Expression rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.operator = operator;
    }
}

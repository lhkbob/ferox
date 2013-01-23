package com.ferox.resource.shader.grammar;

public class AssignmentOperatorExpression implements AssignmentExpression {
    public static enum AssignmentOperator {
        EQUAL,
        MUL_ASSIGN,
        DIV_ASSIGN,
        MOD_ASSIGN,
        ADD_ASSIGN,
        SUB_ASSIGN
        // FIXME insert logical and bitwise assignment operators
    }

    private final UnaryExpression lvalue;
    private final AssignmentOperator operator;
    private final AssignmentExpression rvalue;

    public AssignmentOperatorExpression(UnaryExpression lvalue,
                                        AssignmentOperator operator,
                                        AssignmentExpression rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.operator = operator;
    }
}

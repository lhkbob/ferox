package com.ferox.resource.shader.simple_grammar;

public class Jump implements Statement {
    public static enum JumpType {
        BREAK,
        CONTINUE,
        DISCARD,
        RETURN
    }

    private final JumpType type;
    private final Expression returnExpression; // only non-null for return

    public Jump(JumpType type) {
        this.type = type;
        returnExpression = null;
    }

    public Jump(Expression returnExpression) {
        type = JumpType.RETURN;
        this.returnExpression = returnExpression;
    }
}

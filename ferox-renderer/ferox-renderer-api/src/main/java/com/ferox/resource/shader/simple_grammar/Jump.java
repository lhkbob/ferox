package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;

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

    @Override
    public Environment validate(Environment environment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}

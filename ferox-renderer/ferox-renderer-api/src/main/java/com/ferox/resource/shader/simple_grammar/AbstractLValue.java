package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.LValue;
import com.ferox.resource.shader.Statement;

public abstract class AbstractLValue extends AbstractExpression implements LValue {
    @Override
    public Statement setTo(Expression value) {
        return new Assignment(this, value);
    }
}

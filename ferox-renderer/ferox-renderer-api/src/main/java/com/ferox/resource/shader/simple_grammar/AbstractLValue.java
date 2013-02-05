package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.LValue;

public abstract class AbstractLValue extends AbstractExpression implements LValue {
    @Override
    public Expression setTo(Expression value) {
        return new Assignment(this, value);
    }
}

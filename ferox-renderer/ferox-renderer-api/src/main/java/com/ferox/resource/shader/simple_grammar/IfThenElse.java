package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.IfBuilder;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;

public class IfThenElse implements Statement {
    private final Expression condition;
    private final Statement[] onTrue;
    private final Statement[] onFalse; // null for no else branch

    public IfThenElse(Expression condition, Statement[] onTrue, Statement[] onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
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

    public static class Builder implements IfBuilder {
        private final Expression condition;
        private Statement[] onTrue;

        public Builder(Expression condition) {
            this.condition = condition;
            onTrue = null;
        }

        @Override
        public IfBuilder then(Statement... body) {
            if (onTrue != null) {
                throw new IllegalStateException("Already assigned body for true branch");
            }
            onTrue = body;
            return this;
        }

        @Override
        public Statement fi() {
            if (onTrue == null) {
                throw new IllegalStateException("Must specify a true branch first");
            }
            return new IfThenElse(condition, onTrue, null);
        }

        @Override
        public Statement else_(Statement... body) {
            if (onTrue == null) {
                throw new IllegalStateException("Must specify a true branch first");
            }
            return new IfThenElse(condition, onTrue, body);
        }
    }
}

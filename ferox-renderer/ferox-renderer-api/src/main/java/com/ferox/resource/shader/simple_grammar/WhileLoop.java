package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;
import com.ferox.resource.shader.WhileBuilder;

public class WhileLoop implements Statement {
    private final Expression condition;
    private final Statement[] body;

    public WhileLoop(Expression condition, Statement[] body) {
        this.condition = condition;
        this.body = body;
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

    public static class Builder implements WhileBuilder {
        private final Expression condition;

        public Builder(Expression condition) {
            this.condition = condition;
        }

        @Override
        public Statement do_(Statement... statement) {
            return new WhileLoop(condition, statement);
        }
    }
}

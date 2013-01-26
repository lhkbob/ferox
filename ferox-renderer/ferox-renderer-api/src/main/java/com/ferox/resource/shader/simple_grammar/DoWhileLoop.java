package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.DoWhileBuilder;
import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;

public class DoWhileLoop implements Statement {
    private final Statement[] body;
    private final Expression condition;

    public DoWhileLoop(Statement[] body, Expression condition) {
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

    public static class Builder implements DoWhileBuilder {
        private final Statement[] body;

        public Builder(Statement[] body) {
            this.body = body;
        }

        @Override
        public Statement while_(Expression condition) {
            return new DoWhileLoop(body, condition);
        }
    }
}

package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;
import com.ferox.resource.shader.WhileBuilder;

public class ForLoop implements Statement {
    private final Expression initStatement;
    private final Expression condition; // nullable for no condition
    private final Expression increment; // nullable for no increment
    private final Statement[] body;

    public ForLoop(Expression initStatement, Expression condition, Expression increment,
                   Statement[] body) {
        this.initStatement = initStatement;
        this.condition = condition;
        this.increment = increment;
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
        private final Expression initStatement;
        private final Expression condition;
        private final Expression increment;

        public Builder(Expression initStatement, Expression condition,
                       Expression increment) {
            this.initStatement = initStatement;
            this.condition = condition;
            this.increment = increment;
        }

        @Override
        public Statement do_(Statement... statement) {
            return new ForLoop(initStatement, condition, increment, statement);
        }
    }
}

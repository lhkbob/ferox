package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;

public class ForLoop implements Statement {
    private final Expression initStatement;
    private final Expression condition;
    private final Expression increment;
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
        environment = increment
                .validate(condition.validate(initStatement.validate(environment)));
        if (!condition.getType(environment).equals(PrimitiveType.BOOL)) {
            throw new IllegalStateException(
                    "Loop condition expression must evaluate to a boolean");
        }
        if (increment.containsDeclaration()) {
            throw new IllegalStateException(
                    "Increment expression cannot contain a declaration");
        }

        // validate loop body
        Environment scoped = environment.newScope(true);
        for (int i = 0; i < body.length; i++) {
            scoped = body[i].validate(scoped);
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        accumulator.addLine("for (" + initStatement.emitExpression(accumulator) + "; " +
                            condition.emitExpression(accumulator) + "; " +
                            increment.emitExpression(accumulator) + ") {");
        accumulator.pushIndent();
        for (int i = 0; i < body.length; i++) {
            body[i].emit(accumulator);
        }
        accumulator.popIndent();
        accumulator.addLine("}");
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

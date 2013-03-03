package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;

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
        environment = condition.validate(environment);
        if (condition.getType(environment).equals(PrimitiveType.BOOL)) {
            throw new IllegalStateException(
                    "If condition expression must evaluate to a boolean");
        }
        if (condition.containsDeclaration()) {
            throw new IllegalStateException("If condition cannot contain a declaration");
        }

        // validate true block
        Environment trueScope = environment.newScope(false);
        for (int i = 0; i < onTrue.length; i++) {
            trueScope = onTrue[i].validate(trueScope);
        }

        // validate false block if it exists
        if (onFalse != null) {
            Environment falseScope = environment.newScope(false);
            for (int i = 0; i < onFalse.length; i++) {
                falseScope = onFalse[i].validate(falseScope);
            }
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        accumulator.addLine("if (" + condition.emitExpression(accumulator) + ") {");
        accumulator.pushIndent();
        for (int i = 0; i < onTrue.length; i++) {
            onTrue[i].emit(accumulator);
        }
        accumulator.popIndent();

        if (onFalse != null) {
            accumulator.addLine("} else {");
            accumulator.pushIndent();
            for (int i = 0; i < onFalse.length; i++) {
                onFalse[i].emit(accumulator);
            }
            accumulator.popIndent();
        }

        accumulator.addLine("}");
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

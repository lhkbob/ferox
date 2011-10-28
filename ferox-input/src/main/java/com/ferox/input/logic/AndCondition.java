package com.ferox.input.logic;

public class AndCondition implements Condition {
    private final Condition left;
    private final Condition right;
    
    public AndCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        return left.apply(prev, next) && right.apply(prev, next);
    }
}

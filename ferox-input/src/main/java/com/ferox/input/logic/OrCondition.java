package com.ferox.input.logic;

public class OrCondition implements Condition {
    private final Condition left;
    private final Condition right;
    
    public OrCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        return left.apply(prev, next) || right.apply(prev, next);
    }
}

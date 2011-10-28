package com.ferox.input.logic;

public class NotCondition implements Condition {
    private final Condition not;
    
    public NotCondition(Condition not) {
        this.not = not;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        return !not.apply(prev, next);
    }
}

package com.ferox.input.logic;

public interface ActionBuilder {
    public void trigger(Action action);
    
    public ActionBuilder and(Predicate pred);
    
    public ActionBuilder or(Predicate pred);
}

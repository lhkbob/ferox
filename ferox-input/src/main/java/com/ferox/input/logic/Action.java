package com.ferox.input.logic;

/**
 * Actions represent trigger-able scripts that run when specific conditions or
 * {@link Predicate predicates} are satisfied. Actions are registered with
 * predicates on an {@link InputManager}, which is then processed as part of the
 * main game loop.
 * 
 * @author Michael Ludwig
 */
public interface Action {
    /**
     * <p>
     * Perform whatever action is required. The two InputState arguments are the
     * states that this action's associated predicate evaluated to true with.
     * <p>
     * This will always be called from the thread that invoked
     * {@link InputManager#process()}.
     * 
     * @param prev The previous input state
     * @param next The next input state
     */
    public void perform(InputState prev, InputState next);
}

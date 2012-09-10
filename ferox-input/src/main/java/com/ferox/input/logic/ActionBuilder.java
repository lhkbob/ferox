package com.ferox.input.logic;

/**
 * ActionBuilder is a simple interface used to create a more fluent API when
 * registering Predicates and Actions with an InputManager. An ActionBuilder
 * instance should be discarded after its {@link #trigger(Action)} method is
 * called (which completes the build process).
 * 
 * @author Michael Ludwig
 */
public interface ActionBuilder {
    /**
     * Complete the process of registering a Predicate and Action with the
     * InputManager that returned this builder. The provided action will be
     * invoked as appropriate by the manager using the predicate passed into
     * {@link InputManager#on(Predicate)}.
     * 
     * @param action The action that is executed when the predicate passes
     * @throws NullPointerException if action is null
     */
    public void trigger(Action action);
}

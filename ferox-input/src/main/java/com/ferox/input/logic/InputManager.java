package com.ferox.input.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.ferox.input.KeyEvent;
import com.ferox.input.KeyListener;
import com.ferox.input.MouseEvent;
import com.ferox.input.MouseKeyEventSource;
import com.ferox.input.MouseListener;

/**
 * <p>
 * InputManager provides a higher level input handling API on top of the
 * event-based system in com.ferox.input. It uses {@link Predicate predicates}
 * to determine when to run specific {@link Action actions}. Instead of being
 * executed whenever an event occurs, these are invoked in a controlled manner
 * during the game loop by calling {@link #process()} each frame.
 * <p>
 * Here is example code to configure and use an InputManager:
 * 
 * <pre>
 * MouseEventKeySource window; // however you get one of these (e.g. Framework.createSurface())
 * InputManager manager = new InputManager();
 * manager.on(Predicates.keyPressed(KeyCode.ESCAPE))
 *        .trigger(new Action() { ... });
 * // add any other actions desired
 * manager.attach(window);
 * 
 * while(true) {
 *    manager.process();
 *    // update
 *    // render
 * }
 * </pre>
 * 
 * @author Michael Ludwig
 */
public class InputManager {
    private InputState lastState;
    private InputState lastProcessedState;
    private final Queue<InputState> stateQueue;
    
    private final InternalListener listener; // also acts as synchronization lock
    private final List<PredicatedAction> triggers;
    
    private MouseKeyEventSource source;
    
    /**
     * Create a new InputManager that is not attached to any
     * MouseKeyEventSource, and must be attached before it can process any
     * events. It's still permissible to register actions before attaching to an
     * event source.
     */
    public InputManager() {
        stateQueue = new ArrayDeque<InputState>();
        triggers = new ArrayList<PredicatedAction>();
        
        listener = new InternalListener();
        lastState = new InputState();
        lastProcessedState = lastState;
    }
    
    /**
     * <p>
     * Attach the InputManager to the given MouseKeyEventSource. The manager can
     * only be attached to a single event source at a time and must be detached
     * before listening on another source.
     * <p>
     * After being attached, the manager will listen to all events from the
     * source and accumulate them as a list of {@link InputState state} changes.
     * New states can be processed every frame to trigger actions by calling
     * {@link #process()}.
     * 
     * @param source The source to attach to
     * @throws NullPointerException if source is null
     * @throws IllegalStateException if the manager is currently attached to
     *             another component
     */
    public void attach(MouseKeyEventSource source) {
        if (source == null)
            throw new NullPointerException("Source cannot be null");
        
        synchronized(this) {
            if (this.source != null)
                throw new IllegalStateException("InputManager already attached to another event source");
            
            source.addKeyListener(listener);
            source.addMouseListener(listener);
            
            this.source = source;
        }
    }
    
    /**
     * Detach this InputManager from the event source it's currently attached
     * to. If the adapter is not attached to a component, nothing happens. After
     * detaching, the manager will no longer receive events and calling
     * {@link #process()} will no longer work.
     */
    public void detach() {
        synchronized(this) {
            if (source != null) {
                source.removeKeyListener(listener);
                source.removeMouseListener(listener);
                
                source = null;
            }
        }
    }
    
    /**
     * @return The event source this manager is attached to or null
     */
    public MouseKeyEventSource getEventSource() {
        return source;
    }
    
    /**
     * <p>
     * Begin registering a new action with this InputManager that will be
     * triggered when <tt>predicate</tt> evaluates to true. The action will not
     * be registered until {@link ActionBuilder#trigger(Action)} is called on
     * the returned ActionBuilder.
     * <p>
     * This allows code to read reasonably fluently:
     * <code>manager.on(condition).trigger(action);</code>
     * 
     * @param predicate The predicate that controls when the action is executed
     * @return An ActionBuilder to complete the registering process
     * @throws NullPointerException if predicate is null
     */
    public ActionBuilder on(Predicate predicate) {
        return new ActionBuilderImpl(predicate);
    }
    
    /**
     * Remove or unregister the given action from this manager. If the action
     * was registered with multiple predicates, all occurrences of it will be
     * removed to guarantee that <tt>trigger</tt> can no longer be invoked as a
     * result of calling this manager's {@link #process()} method.
     * 
     * @param trigger The trigger to remove
     * @throws NullPointerException if trigger is null
     */
    public void removeAction(Action trigger) {
        if (trigger == null)
            throw new NullPointerException("Action cannot be null");
        
        synchronized(listener) {
            Iterator<PredicatedAction> it = triggers.iterator();
            while(it.hasNext()) {
                // remove all occurrences of the action
                if (it.next().trigger == trigger) {
                    it.remove();
                }
            }
        }
    }
    
    /**
     * Process all events that have been accumulated since the last call to
     * {@link #process()} and run all actions that are triggered based on their
     * associated predicate. This will run the actions on the calling thread.
     */
    public void process() {
        synchronized(listener) {
            InputState prev = lastProcessedState;
            for (InputState next: stateQueue) {
                processTriggers(prev, next);
                prev = next;
            }
            lastProcessedState = new InputState(lastState);
            processTriggers(prev, lastProcessedState);
            stateQueue.clear();
        }
    }

    private void processTriggers(InputState prev, InputState next) {
        int ct = triggers.size();
        for (int i = 0; i < ct; i++)
            triggers.get(i).apply(prev, next);
    }
    
    // caller must be synchronized on event listener
    private void advanceState(InputState next) {
        lastState = next;
        stateQueue.add(next);
    }
    
    /*
     * Internal class used to listen for events to prevent InputManager being
     * used as a listener directly. It is also the monitor used by each manager.
     */
    private class InternalListener implements KeyListener, MouseListener {
        @Override
        public void handleEvent(KeyEvent event) {
            synchronized(this) {
                advanceState(new InputState(lastState, event));
            }
        }

        @Override
        public void handleEvent(MouseEvent event) {
            synchronized(this) {
                advanceState(new InputState(lastState, event));
            }
        }
    }
    
    /*
     * Internal ActionBuilder implementation
     */
    private class ActionBuilderImpl implements ActionBuilder {
        Predicate condition;
        
        public ActionBuilderImpl(Predicate base) {
            if (base == null)
                throw new NullPointerException("Predicate cannot be null");
            condition = base;
        }
        
        @Override
        public void trigger(Action action) {
            if (action == null)
                throw new NullPointerException("Action cannot be null");
            
            synchronized(listener) {
                triggers.add(new PredicatedAction(action, condition));
            }
        }
    }
    
    /*
     * Simple pair between an action and its triggering predicate
     */
    private static class PredicatedAction {
        final Action trigger;
        final Predicate condition;
        
        public PredicatedAction(Action trigger, Predicate condition) {
            this.trigger = trigger;
            this.condition = condition;
        }
        
        public void apply(InputState prev, InputState next) {
            if (condition.apply(prev, next))
                trigger.perform(prev, next);
        }
    }
}

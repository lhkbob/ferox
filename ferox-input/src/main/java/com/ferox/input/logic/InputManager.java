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

public class InputManager {
    private InputState lastState;
    private InputState lastProcessedState;
    private final Queue<InputState> stateQueue;
    
    private final InternalListener listener; // also acts as synchronization lock
    private final List<PredicatedAction> triggers;
    
    private final MouseKeyEventSource source;
    
    public InputManager(MouseKeyEventSource source) {
        if (source == null)
            throw new NullPointerException("Source cannot be null");
        
        stateQueue = new ArrayDeque<InputState>();
        triggers = new ArrayList<PredicatedAction>();
        
        listener = new InternalListener();
        lastState = new InputState();
        lastProcessedState = new InputState();
        
        source.addKeyListener(listener);
        source.addMouseListener(listener);
        this.source = source;
    }
    
    public MouseKeyEventSource getEventSource() {
        return source;
    }
    
    public ActionBuilder on(Predicate predicate) {
        return new ActionBuilderImpl(predicate);
    }
    
    public void removeAction(Action trigger) {
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

        @Override
        public ActionBuilder and(Predicate pred) {
            condition = Predicates.and(condition, pred);
            return this;
        }

        @Override
        public ActionBuilder or(Predicate pred) {
            condition = Predicates.or(condition, pred);
            return this;
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

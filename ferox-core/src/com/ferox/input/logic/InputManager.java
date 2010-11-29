package com.ferox.input.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
    private final List<TriggerAndCondition> triggers;
    
    private MouseKeyEventSource source;
    
    public InputManager(MouseKeyEventSource source) {
        stateQueue = new ArrayDeque<InputState>();
        triggers = new ArrayList<TriggerAndCondition>();
        
        listener = new InternalListener();
        lastState = new InputState();
        lastProcessedState = new InputState();
        setEventSource(source);
    }
    
    public MouseKeyEventSource getEventSource() {
        return source;
    }
    
    public void setEventSource(MouseKeyEventSource source) {
        synchronized(listener) {
            if (source == null) {
                if (this.source != null) {
                    this.source.removeKeyListener(listener);
                    this.source.removeMouseListener(listener);
                    this.source = null;
                }
            } else {
                if (this.source != null) {
                    if (this.source == source)
                        return; // nothing to do
                    this.source.removeKeyListener(listener);
                    this.source.removeMouseListener(listener);
                }

                source.addKeyListener(listener);
                source.addMouseListener(listener);
                this.source = source;
            }
        }
    }
    
    public void addTrigger(Trigger trigger, Condition condition) {
        if (trigger == null)
            throw new NullPointerException("Trigger cannot be null");
        if (condition == null)
            throw new NullPointerException("Condition cannot be null");
        
        synchronized(listener) {
            int ct = triggers.size();
            for (int i = 0; i < ct; i++) {
                if (triggers.get(i).trigger == trigger) {
                    triggers.get(i).condition = condition;
                    return;
                }
            }
            
            triggers.add(new TriggerAndCondition(trigger, condition));
        }
    }
    
    public void removeTrigger(Trigger trigger) {
        synchronized(listener) {
            int ct = triggers.size();
            for (int i = 0; i < ct; i++) {
                if (triggers.get(i) == trigger) {
                    triggers.remove(i);
                    return;
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
    
    /*
     * Internal class used to listen for events, so that InputManager can
     * properly control which EventSource it is listening to
     */
    private class InternalListener implements KeyListener, MouseListener {
        @Override
        public void handleEvent(KeyEvent event) {
            synchronized(this) {
                InputState next = new InputState(lastState, event);
                lastState = next;
                stateQueue.add(next);
            }
        }

        @Override
        public void handleEvent(MouseEvent event) {
            synchronized(this) {
                InputState next = new InputState(lastState, event);
                lastState = next;
                stateQueue.add(next);
            }
        }
    }
    
    private static class TriggerAndCondition {
        Trigger trigger;
        Condition condition;
        
        public TriggerAndCondition(Trigger trigger, Condition condition) {
            this.trigger = trigger;
            this.condition = condition;
        }
        
        public void apply(InputState prev, InputState next) {
            if (condition.apply(prev, next))
                trigger.onTrigger(prev, next);
        }
    }
}

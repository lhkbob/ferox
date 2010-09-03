package com.ferox.input;

public interface EventSource {
    public EventDispatcher getDispatcher();
    
    public EventQueue getQueue();
}

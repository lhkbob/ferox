package com.ferox.input;

public interface KeyEventSource extends EventSource {
    public void addKeyListener(KeyListener listener);
    
    public void removeKeyListener(KeyListener listener);
}

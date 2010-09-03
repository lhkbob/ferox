package com.ferox.input;

public interface MouseEventSource extends EventSource {
    public void addMouseListener(MouseListener listener);
    
    public void removeMouseListener(MouseListener listener);
}

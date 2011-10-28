package com.ferox.input;

public class MouseEvent implements Event {
    public static enum Type {
        MOVE, PRESS, RELEASE, SCROLL
    }
    
    public static enum MouseButton {
        NONE, LEFT, RIGHT, CENTER
    }
    
    private final MouseEventSource source;
    private final Type type;
    
    private final int x;
    private final int y;
    private final int scrollDelta;
    
    private final MouseButton button;
    
    public MouseEvent(Type type, MouseEventSource source, int x, int y, int scrollDelta, MouseButton button) {
        if (source == null)
            throw new NullPointerException("Event source cannot be null");
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (button == null)
            throw new IllegalArgumentException("MouseButton cannot be null");
        
        this.source = source;
        this.type = type;
        this.x = x;
        this.y = y;
        this.scrollDelta = scrollDelta;
        this.button = button;
    }
    
    public Type getEventType() {
        return type;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getScrollDelta() {
        return scrollDelta;
    }
    
    public MouseButton getButton() {
        return button;
    }
    
    @Override
    public MouseEventSource getSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return "[Mouse " + type + " at (" + x + ", " + y + "), button: " + button + ", scroll: " + scrollDelta + "]";
    }
}

package com.ferox.input.logic;

import java.util.EnumSet;

import com.ferox.input.MouseEvent;
import com.ferox.input.MouseEvent.MouseButton;
import com.ferox.input.MouseEvent.Type;

public class MouseState {
    private final int x;
    private final int y;
    
    private final int dx;
    private final int dy;
    
    private final EnumSet<MouseButton> buttonsDown;
    private final int scrollCount;
    
    public MouseState() {
        x = 0;
        y = 0;
        
        dx = 0;
        dy = 0;
        
        buttonsDown = EnumSet.noneOf(MouseButton.class);
        scrollCount = 0;
    }
    
    public MouseState(MouseState prev) {
        x = prev.x;
        y = prev.y;
        
        dx = 0;
        dy = 0;
        
        buttonsDown = EnumSet.copyOf(prev.buttonsDown);
        scrollCount = 0;
    }
    
    public MouseState(MouseState prev, MouseEvent event) {
        x = event.getX();
        y = event.getY();
        
        if (prev != null) {
            dx = x - prev.x;
            dy = y - prev.y;
        } else {
            dx = 0;
            dy = 0;
        }
        
        if (prev != null)
            buttonsDown = EnumSet.copyOf(prev.buttonsDown);
        else
            buttonsDown = EnumSet.noneOf(MouseButton.class);
        
        if (event.getEventType() == Type.PRESS)
            buttonsDown.add(event.getButton());
        else if (event.getEventType() == Type.RELEASE)
            buttonsDown.remove(event.getButton());
        
        if (event.getEventType() == Type.SCROLL)
            scrollCount = event.getScrollDelta();
        else
            scrollCount = 0;
    }
    
    public boolean isButtonDown(MouseButton button) {
        if (button == MouseButton.NONE)
            return buttonsDown.isEmpty();
        else
            return buttonsDown.contains(button);
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getDeltaX() {
        return dx;
    }
    
    public int getDeltaY() {
        return dy;
    }
    
    public int getScrollDelta() {
        return scrollCount;
    }
}

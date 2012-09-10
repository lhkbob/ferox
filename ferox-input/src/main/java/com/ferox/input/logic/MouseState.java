package com.ferox.input.logic;

import java.util.EnumSet;

import com.ferox.input.MouseEvent;
import com.ferox.input.MouseEvent.MouseButton;
import com.ferox.input.MouseEvent.Type;

/**
 * MouseState represents an immutable snapshot of the user's mouse. It can be
 * used to query if any button is held down or released, as well as the current
 * mouse location and scroll position.
 * 
 * @author Michael Ludwig
 */
public class MouseState {
    private final int x;
    private final int y;
    
    private final EnumSet<MouseButton> buttonsDown;
    private final int scrollCount;
    
    /**
     * Create a new MouseState that starts at (0, 0) and has an initial scroll
     * value of 0.
     */
    public MouseState() {
        x = 0;
        y = 0;
        
        buttonsDown = EnumSet.noneOf(MouseButton.class);
        scrollCount = 0;
    }
    
    /**
     * <p>
     * Create a new MouseState that will mark a button as down if the event is a
     * PRESS, or will mark it as up if the event is a RELEASE. Any other button
     * marked as up or down from the previous state will be kept unchanged.
     * <p>
     * The state's x and y positions will be updated to match the events and the
     * scroll count will be updated by the event's delta.
     * 
     * @param prev The previous mouse state before the event
     * @param event The mouse event to update to the new state
     * @throws NullPointerException if event is null
     */
    public MouseState(MouseState prev, MouseEvent event) {
        x = event.getX();
        y = event.getY();
        
        if (prev != null)
            buttonsDown = EnumSet.copyOf(prev.buttonsDown);
        else
            buttonsDown = EnumSet.noneOf(MouseButton.class);
        
        if (event.getEventType() == Type.PRESS)
            buttonsDown.add(event.getButton());
        else if (event.getEventType() == Type.RELEASE)
            buttonsDown.remove(event.getButton());
        
        if (event.getEventType() == Type.SCROLL)
            scrollCount = prev.scrollCount + event.getScrollDelta();
        else
            scrollCount = prev.scrollCount;
    }
    
    /**
     * <p>
     * Return whether or not the given mouse button is pressed as of this state.
     * The event that marked the button as down could have happened any number
     * of frames in the past, but if it is down then it is guaranteed that a
     * release for that button hasn't been received as of this state's
     * timestamp.
     * <p>
     * The NONE button is handled slightly differently from the other real
     * buttons. NONE will return true if-and-only-if no other mouse button is
     * pressed.
     * 
     * @param button The button that might be down
     * @return True if the button is currently held down
     */
    public boolean isButtonDown(MouseButton button) {
        if (button == MouseButton.NONE)
            return buttonsDown.isEmpty();
        else
            return buttonsDown.contains(button);
    }
    
    /**
     * @return The current x position of the mouse
     */
    public int getX() {
        return x;
    }
    
    /**
     * @return The current y position of the mouse
     */
    public int getY() {
        return y;
    }
    
    /**
     * <p>
     * Get the number of scroll ticks that have been performed as of this state.
     * This does not represent a delta of scroll ticks, like
     * {@link MouseEvent#getScrollDelta()}, but is instead the cumulative total
     * of deltas from all scroll events to reach this state.
     * <p>
     * Scroll delta can be computed by subtracting the next and previous state
     * counts passed to {@link Predicate#apply(InputState, InputState)}.
     * 
     * @return The count of accumulated scroll ticks performed to reach this
     *         state
     */
    public int getScrollCount() {
        return scrollCount;
    }
}

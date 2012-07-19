package com.ferox.renderer.impl.lwjgl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.ferox.input.AWTEventAdapter;
import com.ferox.input.Event;
import com.ferox.input.EventDispatcher;
import com.ferox.input.EventQueue;
import com.ferox.input.KeyEvent;
import com.ferox.input.KeyListener;
import com.ferox.input.MouseEvent;
import com.ferox.input.MouseKeyEventSource;
import com.ferox.input.MouseListener;

/**
 * A MouseKeyEventSource that wraps the static {@link Mouse} and
 * {@link Keyboard}, and mimics another MouseKeyEventSource that actually
 * triggers the events. This is purely a helper implementation for mapping the
 * events, much like the {@link AWTEventAdapter}.
 * 
 * @author Michael Ludwig
 */
public class LwjglInputEventAdapter implements MouseKeyEventSource {
    private final MouseKeyEventSource realSource;
    private final EventDispatcher dispatcher;
    
    private final List<KeyListener> keyListeners;
    private final List<MouseListener> mouseListeners;
    
    private LwjglInputPoller poller;
    
    public LwjglInputEventAdapter(MouseKeyEventSource realSource) {
        if (realSource == null)
            throw new NullPointerException("EventSource cannot be null");
        
        this.realSource = realSource;
        
        dispatcher = new LwjglEventDispatcher();
        keyListeners = new ArrayList<KeyListener>();
        mouseListeners = new ArrayList<MouseListener>();
    }
    
    public void startPolling() {
        synchronized(this) {
            if (poller != null)
                throw new IllegalStateException("Already polling inputs");
            
            poller = new LwjglInputPoller();
            Thread pollThread = new Thread(poller, "LWJGL Input Poller");
            pollThread.setDaemon(true);
            pollThread.start();
        }
    }
    
    public void stopPolling() {
        synchronized(this) {
            if (poller == null)
                throw new IllegalStateException("Not polling inputs");
            
            poller.shutdown = true;
            poller = null;
        }
    }
    
    @Override
    public void addMouseListener(MouseListener listener) {
        if (listener == null)
            throw new NullPointerException("MouseListener cannot be null");
        synchronized(this) {
            if (!mouseListeners.contains(listener))
                mouseListeners.add(listener);
        }
    }

    @Override
    public void removeMouseListener(MouseListener listener) {
        if (listener == null)
            throw new NullPointerException("MouseListener cannot be null");
        synchronized(this) {
            mouseListeners.remove(listener);
        }
    }

    @Override
    public EventQueue getQueue() {
        return realSource.getQueue();
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        if (listener == null)
            throw new NullPointerException("KeyListener cannot be null");
        synchronized(this) {
            if (!keyListeners.contains(listener))
                keyListeners.add(listener);
        }
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        if (listener == null)
            throw new NullPointerException("KeyListener cannot be null");
        synchronized(this) {
            keyListeners.remove(listener);
        }
    }
    
    private class LwjglInputPoller implements Runnable {
        private volatile boolean shutdown;
        
        @Override
        public void run() {
            // make sure to allow negative coordinates to be compatible with AWT's
            // mouse event generation
            Mouse.setClipMouseCoordinatesToWindow(false);
            
            int lastMouseX = Integer.MIN_VALUE;
            int lastMouseY = Integer.MIN_VALUE;
            
            while(!shutdown) {
                // Must call processMessages() to read new events from the OS
                Display.processMessages();
                
                // Process all incoming mouse events
                // FIXME race condition exists if shutdown occurs after above
                // boolean check, and an exception is thrown (should just eat it)
                // - IllegalStateException
                while(Mouse.next()) {
                    int x = Mouse.getEventX();
                    int y = Mouse.getEventY();
                    
                    if (x != lastMouseX || y != lastMouseY) {
                        // push a mouse-moved event
                        getQueue().postEvent(new MouseEvent(MouseEvent.Type.MOVE, realSource,
                                                            x, y, 0, MouseEvent.MouseButton.NONE), dispatcher);
                    }
                    
                    int scrollDelta = Mouse.getEventDWheel();
                    if (scrollDelta != 0) {
                        // push a mouse-wheel-scroll event
                        getQueue().postEvent(new MouseEvent(MouseEvent.Type.SCROLL, realSource, 
                                                            x, y, 0, MouseEvent.MouseButton.NONE), dispatcher);
                    }
                    
                    switch(Mouse.getEventButton()) {
                    case 0: {
                        MouseEvent.Type type = (Mouse.getEventButtonState() ? MouseEvent.Type.PRESS : MouseEvent.Type.RELEASE);
                        getQueue().postEvent(new MouseEvent(type, realSource, x, y, 0, MouseEvent.MouseButton.LEFT), dispatcher);
                        break; }
                    case 1:{
                        MouseEvent.Type type = (Mouse.getEventButtonState() ? MouseEvent.Type.PRESS : MouseEvent.Type.RELEASE);
                        getQueue().postEvent(new MouseEvent(type, realSource, x, y, 0, MouseEvent.MouseButton.RIGHT), dispatcher);
                        break; }
                    case 2:{
                        MouseEvent.Type type = (Mouse.getEventButtonState() ? MouseEvent.Type.PRESS : MouseEvent.Type.RELEASE);
                        getQueue().postEvent(new MouseEvent(type, realSource, x, y, 0, MouseEvent.MouseButton.CENTER), dispatcher);
                        break; }
                    default:
                        // no button event
                        break;
                    }
                }
                
                // Process all incoming keyboard events
                while(Keyboard.next()) {
                    KeyEvent.KeyCode keyCode = getKeyCode(Keyboard.getEventKey());
                    
                    char eventChar = Keyboard.getEventCharacter();
                    if (eventChar == Keyboard.CHAR_NONE)
                        eventChar = KeyEvent.CHAR_UNKNOWN;
                    
                    KeyEvent.Type type = (Keyboard.getEventKeyState() ? KeyEvent.Type.PRESS : KeyEvent.Type.RELEASE);
                    
                    getQueue().postEvent(new KeyEvent(type, realSource, keyCode, eventChar), dispatcher);
                }
                
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // do nothing
                }
            }
        }
        
        private KeyEvent.KeyCode getKeyCode(int code) {
            switch(code) {
            case Keyboard.KEY_ESCAPE: return KeyEvent.KeyCode.ESCAPE;
            case Keyboard.KEY_GRAVE: return KeyEvent.KeyCode.BACK_QUOTE;
            case Keyboard.KEY_TAB: return KeyEvent.KeyCode.TAB;
            case Keyboard.KEY_LBRACKET: return KeyEvent.KeyCode.LEFT_BRACKET;
            case Keyboard.KEY_RBRACKET: return KeyEvent.KeyCode.RIGHT_BRACKET;
            case Keyboard.KEY_BACKSLASH: return KeyEvent.KeyCode.BACK_SLASH;
            case Keyboard.KEY_SLASH: return KeyEvent.KeyCode.FORWARD_SLASH;
            case Keyboard.KEY_RETURN: return KeyEvent.KeyCode.RETURN;
            case Keyboard.KEY_SEMICOLON: return KeyEvent.KeyCode.SEMICOLON;
            case Keyboard.KEY_APOSTROPHE: return KeyEvent.KeyCode.QUOTE;
            case Keyboard.KEY_COMMA: return KeyEvent.KeyCode.COMMA;
            case Keyboard.KEY_PERIOD: return KeyEvent.KeyCode.PERIOD;
            case Keyboard.KEY_MINUS: return KeyEvent.KeyCode.MINUS;
            case Keyboard.KEY_EQUALS: return KeyEvent.KeyCode.EQUALS;
            case Keyboard.KEY_BACK: return KeyEvent.KeyCode.BACK_SPACE;
            case Keyboard.KEY_DELETE: return KeyEvent.KeyCode.DELETE;
            
            case Keyboard.KEY_SPACE: return KeyEvent.KeyCode.SPACE;
            
            case Keyboard.KEY_PAUSE: return KeyEvent.KeyCode.PAUSE;
            case Keyboard.KEY_INSERT: return KeyEvent.KeyCode.INSERT;

            case Keyboard.KEY_HOME: return KeyEvent.KeyCode.HOME;
            case Keyboard.KEY_END: return KeyEvent.KeyCode.END;
            case Keyboard.KEY_PRIOR: return KeyEvent.KeyCode.PAGE_UP;
            case Keyboard.KEY_NEXT: return KeyEvent.KeyCode.PAGE_DOWN;
            
            case Keyboard.KEY_UP: return KeyEvent.KeyCode.UP;
            case Keyboard.KEY_LEFT: return KeyEvent.KeyCode.LEFT;
            case Keyboard.KEY_RIGHT: return KeyEvent.KeyCode.RIGHT;
            case Keyboard.KEY_DOWN: return KeyEvent.KeyCode.DOWN;
            
            case Keyboard.KEY_F1: return KeyEvent.KeyCode.F1;
            case Keyboard.KEY_F2: return KeyEvent.KeyCode.F2;
            case Keyboard.KEY_F3: return KeyEvent.KeyCode.F3;
            case Keyboard.KEY_F4: return KeyEvent.KeyCode.F4;
            case Keyboard.KEY_F5: return KeyEvent.KeyCode.F5;
            case Keyboard.KEY_F6: return KeyEvent.KeyCode.F6;
            case Keyboard.KEY_F7: return KeyEvent.KeyCode.F7;
            case Keyboard.KEY_F8: return KeyEvent.KeyCode.F8;
            case Keyboard.KEY_F9: return KeyEvent.KeyCode.F9;
            case Keyboard.KEY_F10: return KeyEvent.KeyCode.F10;
            case Keyboard.KEY_F11: return KeyEvent.KeyCode.F11;
            case Keyboard.KEY_F12: return KeyEvent.KeyCode.F12;

            
            case Keyboard.KEY_0: return KeyEvent.KeyCode.N0;
            case Keyboard.KEY_1: return KeyEvent.KeyCode.N1;
            case Keyboard.KEY_2: return KeyEvent.KeyCode.N2;
            case Keyboard.KEY_3: return KeyEvent.KeyCode.N3;
            case Keyboard.KEY_4: return KeyEvent.KeyCode.N4;
            case Keyboard.KEY_5: return KeyEvent.KeyCode.N5;
            case Keyboard.KEY_6: return KeyEvent.KeyCode.N6;
            case Keyboard.KEY_7: return KeyEvent.KeyCode.N7;
            case Keyboard.KEY_8: return KeyEvent.KeyCode.N8;
            case Keyboard.KEY_9: return KeyEvent.KeyCode.N9;
            
            case Keyboard.KEY_A: return KeyEvent.KeyCode.A;
            case Keyboard.KEY_B: return KeyEvent.KeyCode.B;
            case Keyboard.KEY_C: return KeyEvent.KeyCode.C;
            case Keyboard.KEY_D: return KeyEvent.KeyCode.D;
            case Keyboard.KEY_E: return KeyEvent.KeyCode.E;
            case Keyboard.KEY_F: return KeyEvent.KeyCode.F;
            case Keyboard.KEY_G: return KeyEvent.KeyCode.G;
            case Keyboard.KEY_H: return KeyEvent.KeyCode.H;
            case Keyboard.KEY_I: return KeyEvent.KeyCode.I;
            case Keyboard.KEY_J: return KeyEvent.KeyCode.J;
            case Keyboard.KEY_K: return KeyEvent.KeyCode.K;
            case Keyboard.KEY_L: return KeyEvent.KeyCode.L;
            case Keyboard.KEY_M: return KeyEvent.KeyCode.M;
            case Keyboard.KEY_N: return KeyEvent.KeyCode.N;
            case Keyboard.KEY_O: return KeyEvent.KeyCode.O;
            case Keyboard.KEY_P: return KeyEvent.KeyCode.P;
            case Keyboard.KEY_Q: return KeyEvent.KeyCode.Q;
            case Keyboard.KEY_R: return KeyEvent.KeyCode.R;
            case Keyboard.KEY_S: return KeyEvent.KeyCode.S;
            case Keyboard.KEY_T: return KeyEvent.KeyCode.T;
            case Keyboard.KEY_U: return KeyEvent.KeyCode.U;
            case Keyboard.KEY_V: return KeyEvent.KeyCode.V;
            case Keyboard.KEY_W: return KeyEvent.KeyCode.W;
            case Keyboard.KEY_X: return KeyEvent.KeyCode.X;
            case Keyboard.KEY_Y: return KeyEvent.KeyCode.Y;
            case Keyboard.KEY_Z: return KeyEvent.KeyCode.Z;

            case Keyboard.KEY_NUMLOCK: return KeyEvent.KeyCode.NUM_LOCK;
            case Keyboard.KEY_SCROLL: return KeyEvent.KeyCode.SCROLL_LOCK;
            case Keyboard.KEY_CAPITAL: return KeyEvent.KeyCode.CAPS_LOCK;

            case Keyboard.KEY_NUMPAD1: return KeyEvent.KeyCode.NUMPAD_1;
            case Keyboard.KEY_NUMPAD2: return KeyEvent.KeyCode.NUMPAD_2;
            case Keyboard.KEY_NUMPAD3: return KeyEvent.KeyCode.NUMPAD_3;
            case Keyboard.KEY_NUMPAD4: return KeyEvent.KeyCode.NUMPAD_4;
            case Keyboard.KEY_NUMPAD5: return KeyEvent.KeyCode.NUMPAD_5;
            case Keyboard.KEY_NUMPAD6: return KeyEvent.KeyCode.NUMPAD_6;
            case Keyboard.KEY_NUMPAD7: return KeyEvent.KeyCode.NUMPAD_7;
            case Keyboard.KEY_NUMPAD8: return KeyEvent.KeyCode.NUMPAD_8;
            case Keyboard.KEY_NUMPAD9: return KeyEvent.KeyCode.NUMPAD_9;
            case Keyboard.KEY_NUMPAD0: return KeyEvent.KeyCode.NUMPAD_0;
            case Keyboard.KEY_ADD: return KeyEvent.KeyCode.NUMPAD_ADD;
            case Keyboard.KEY_SUBTRACT: return KeyEvent.KeyCode.NUMPAD_SUBTRACT;
            case Keyboard.KEY_DECIMAL: return KeyEvent.KeyCode.NUMPAD_DECIMAL;
            case Keyboard.KEY_DIVIDE: return KeyEvent.KeyCode.NUMPAD_DIVIDE;
            case Keyboard.KEY_MULTIPLY: return KeyEvent.KeyCode.NUMPAD_MULTIPLY;
            
            case Keyboard.KEY_LMENU: return KeyEvent.KeyCode.LEFT_ALT;
            case Keyboard.KEY_RMENU: return KeyEvent.KeyCode.RIGHT_ALT;
            case Keyboard.KEY_LCONTROL: return KeyEvent.KeyCode.LEFT_CONTROL;
            case Keyboard.KEY_RCONTROL: return KeyEvent.KeyCode.RIGHT_CONTROL;
            case Keyboard.KEY_LSHIFT: return KeyEvent.KeyCode.LEFT_SHIFT;
            case Keyboard.KEY_RSHIFT: return KeyEvent.KeyCode.RIGHT_SHIFT;
            case Keyboard.KEY_LMETA: return KeyEvent.KeyCode.LEFT_META;
            case Keyboard.KEY_RMETA: return KeyEvent.KeyCode.RIGHT_META;
            default:
                return KeyEvent.KeyCode.UNKNOWN;
            }
        }
    }
    
    /* 
     * Internal class handling the event dispatching to the
     * added Key and MouseListeners.
     */
    private class LwjglEventDispatcher implements EventDispatcher {
        @Override
        public void dispatchEvent(Event e) {
            if (e instanceof KeyEvent) {
                synchronized(this) {
                    KeyEvent ke = (KeyEvent) e;
                    int size = keyListeners.size();
                    for (int i = 0; i < size; i++)
                        keyListeners.get(i).handleEvent(ke);
                }
            } else if (e instanceof MouseEvent) {
                synchronized(this) {
                    MouseEvent me = (MouseEvent) e;
                    int size = mouseListeners.size();
                    for (int i = 0; i < size; i++)
                        mouseListeners.get(i).handleEvent(me);
                }
            } // else someone is tampering with events, just ignore it
        }
    }
}

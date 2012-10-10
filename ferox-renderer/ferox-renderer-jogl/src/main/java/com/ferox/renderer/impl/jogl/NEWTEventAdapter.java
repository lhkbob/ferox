/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.jogl;

import com.ferox.input.AWTEventAdapter;
import com.ferox.input.KeyEvent;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.MouseEvent;
import com.ferox.input.MouseEvent.MouseButton;
import com.ferox.input.MouseKeyEventDispatcher;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;

/**
 * NEWTEventAdapter is a utility for converting events produced by the NEWT
 * framework to the appropriate events for this framework, much like the
 * {@link AWTEventAdapter}.
 * 
 * @author Michael Ludwig
 */
public class NEWTEventAdapter implements KeyListener, MouseListener {
    private Window component;

    private final MouseKeyEventDispatcher dispatcher;

    /**
     * Create a new AWTEventAdapter that will convert AWT events and dispatch
     * them to the given MouseKeyEventDispatcher.
     * 
     * @param dispatcher The dispatcher to use
     */
    public NEWTEventAdapter(MouseKeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new NullPointerException("Dispatcher cannot be null");
        }

        this.dispatcher = dispatcher;
    }

    /**
     * <p>
     * Attach the adapter to the given component. The adapter can only be
     * attached to a single component at a time and must be detached before
     * listening on another component.
     * <p>
     * To produce meaningful events, the attached component must be related to
     * the EventSource implementation used by this adapter's dispatcher.
     * <p>
     * The adapter cannot send events to its dispatcher until its been attached
     * to an alive component that is producing the relevant AWT events.
     * 
     * @param component The component to attach to
     * @throws NullPointerException if component is null
     * @throws IllegalStateException if the adapter is currently attached to
     *             another component
     */
    public void attach(Window component) {
        if (component == null) {
            throw new NullPointerException("Window cannot be null");
        }

        synchronized (this) {
            if (this.component != null) {
                throw new IllegalStateException("NEWTEventAdapter already attached to another Window");
            }

            component.addKeyListener(this);
            component.addMouseListener(this);

            this.component = component;
        }
    }

    /**
     * Detach this adapter from the window it's currently attached to. If the
     * adapter is not attached to a component, nothing happens. After detaching,
     * the adapter will not convert and dispatch AWT events.
     */
    public void detach() {
        synchronized (this) {
            if (component != null) {
                component.removeKeyListener(this);
                component.removeMouseListener(this);

                component = null;
            }
        }
    }

    /* AWT event listener methods */

    private KeyCode getKeyCode(com.jogamp.newt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
        case com.jogamp.newt.event.KeyEvent.VK_ESCAPE:
            return KeyCode.ESCAPE;
        case com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE:
            return KeyCode.BACK_QUOTE;
        case com.jogamp.newt.event.KeyEvent.VK_TAB:
            return KeyCode.TAB;
        case com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET:
            return KeyCode.LEFT_BRACKET;
        case com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET:
            return KeyCode.RIGHT_BRACKET;
        case com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH:
            return KeyCode.BACK_SLASH;
        case com.jogamp.newt.event.KeyEvent.VK_SLASH:
            return KeyCode.FORWARD_SLASH;
        case com.jogamp.newt.event.KeyEvent.VK_ENTER:
            return KeyCode.RETURN;
        case com.jogamp.newt.event.KeyEvent.VK_SEMICOLON:
            return KeyCode.SEMICOLON;
        case com.jogamp.newt.event.KeyEvent.VK_QUOTE:
            return KeyCode.QUOTE;
        case com.jogamp.newt.event.KeyEvent.VK_COMMA:
            return KeyCode.COMMA;
        case com.jogamp.newt.event.KeyEvent.VK_PERIOD:
            return KeyCode.PERIOD;
        case com.jogamp.newt.event.KeyEvent.VK_MINUS:
            return KeyCode.MINUS;
        case com.jogamp.newt.event.KeyEvent.VK_EQUALS:
            return KeyCode.EQUALS;
        case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE:
            return KeyCode.BACK_SPACE;
        case com.jogamp.newt.event.KeyEvent.VK_DELETE:
            return KeyCode.DELETE;

        case com.jogamp.newt.event.KeyEvent.VK_SPACE:
            return KeyCode.SPACE;

        case com.jogamp.newt.event.KeyEvent.VK_PAUSE:
            return KeyCode.PAUSE;
        case com.jogamp.newt.event.KeyEvent.VK_INSERT:
            return KeyCode.INSERT;

        case com.jogamp.newt.event.KeyEvent.VK_HOME:
            return KeyCode.HOME;
        case com.jogamp.newt.event.KeyEvent.VK_END:
            return KeyCode.END;
        case com.jogamp.newt.event.KeyEvent.VK_PAGE_UP:
            return KeyCode.PAGE_UP;
        case com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN:
            return KeyCode.PAGE_DOWN;

        case com.jogamp.newt.event.KeyEvent.VK_UP:
            return KeyCode.UP;
        case com.jogamp.newt.event.KeyEvent.VK_LEFT:
            return KeyCode.LEFT;
        case com.jogamp.newt.event.KeyEvent.VK_RIGHT:
            return KeyCode.RIGHT;
        case com.jogamp.newt.event.KeyEvent.VK_DOWN:
            return KeyCode.DOWN;

        case com.jogamp.newt.event.KeyEvent.VK_F1:
            return KeyCode.F1;
        case com.jogamp.newt.event.KeyEvent.VK_F2:
            return KeyCode.F2;
        case com.jogamp.newt.event.KeyEvent.VK_F3:
            return KeyCode.F3;
        case com.jogamp.newt.event.KeyEvent.VK_F4:
            return KeyCode.F4;
        case com.jogamp.newt.event.KeyEvent.VK_F5:
            return KeyCode.F5;
        case com.jogamp.newt.event.KeyEvent.VK_F6:
            return KeyCode.F6;
        case com.jogamp.newt.event.KeyEvent.VK_F7:
            return KeyCode.F7;
        case com.jogamp.newt.event.KeyEvent.VK_F8:
            return KeyCode.F8;
        case com.jogamp.newt.event.KeyEvent.VK_F9:
            return KeyCode.F9;
        case com.jogamp.newt.event.KeyEvent.VK_F10:
            return KeyCode.F10;
        case com.jogamp.newt.event.KeyEvent.VK_F11:
            return KeyCode.F11;
        case com.jogamp.newt.event.KeyEvent.VK_F12:
            return KeyCode.F12;

        case com.jogamp.newt.event.KeyEvent.VK_1:
            return KeyCode.N1;
        case com.jogamp.newt.event.KeyEvent.VK_2:
            return KeyCode.N2;
        case com.jogamp.newt.event.KeyEvent.VK_3:
            return KeyCode.N3;
        case com.jogamp.newt.event.KeyEvent.VK_4:
            return KeyCode.N4;
        case com.jogamp.newt.event.KeyEvent.VK_5:
            return KeyCode.N5;
        case com.jogamp.newt.event.KeyEvent.VK_6:
            return KeyCode.N6;
        case com.jogamp.newt.event.KeyEvent.VK_7:
            return KeyCode.N7;
        case com.jogamp.newt.event.KeyEvent.VK_8:
            return KeyCode.N8;
        case com.jogamp.newt.event.KeyEvent.VK_9:
            return KeyCode.N9;
        case com.jogamp.newt.event.KeyEvent.VK_0:
            return KeyCode.N0;

        case com.jogamp.newt.event.KeyEvent.VK_A:
            return KeyCode.A;
        case com.jogamp.newt.event.KeyEvent.VK_B:
            return KeyCode.B;
        case com.jogamp.newt.event.KeyEvent.VK_C:
            return KeyCode.C;
        case com.jogamp.newt.event.KeyEvent.VK_D:
            return KeyCode.D;
        case com.jogamp.newt.event.KeyEvent.VK_E:
            return KeyCode.E;
        case com.jogamp.newt.event.KeyEvent.VK_F:
            return KeyCode.F;
        case com.jogamp.newt.event.KeyEvent.VK_G:
            return KeyCode.G;
        case com.jogamp.newt.event.KeyEvent.VK_H:
            return KeyCode.H;
        case com.jogamp.newt.event.KeyEvent.VK_I:
            return KeyCode.I;
        case com.jogamp.newt.event.KeyEvent.VK_J:
            return KeyCode.J;
        case com.jogamp.newt.event.KeyEvent.VK_K:
            return KeyCode.K;
        case com.jogamp.newt.event.KeyEvent.VK_L:
            return KeyCode.L;
        case com.jogamp.newt.event.KeyEvent.VK_M:
            return KeyCode.M;
        case com.jogamp.newt.event.KeyEvent.VK_N:
            return KeyCode.N;
        case com.jogamp.newt.event.KeyEvent.VK_O:
            return KeyCode.O;
        case com.jogamp.newt.event.KeyEvent.VK_P:
            return KeyCode.P;
        case com.jogamp.newt.event.KeyEvent.VK_Q:
            return KeyCode.Q;
        case com.jogamp.newt.event.KeyEvent.VK_R:
            return KeyCode.R;
        case com.jogamp.newt.event.KeyEvent.VK_S:
            return KeyCode.S;
        case com.jogamp.newt.event.KeyEvent.VK_T:
            return KeyCode.T;
        case com.jogamp.newt.event.KeyEvent.VK_U:
            return KeyCode.U;
        case com.jogamp.newt.event.KeyEvent.VK_V:
            return KeyCode.V;
        case com.jogamp.newt.event.KeyEvent.VK_W:
            return KeyCode.W;
        case com.jogamp.newt.event.KeyEvent.VK_X:
            return KeyCode.X;
        case com.jogamp.newt.event.KeyEvent.VK_Y:
            return KeyCode.Y;
        case com.jogamp.newt.event.KeyEvent.VK_Z:
            return KeyCode.Z;

        case com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK:
            return KeyCode.NUM_LOCK;
        case com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK:
            return KeyCode.SCROLL_LOCK;
        case com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK:
            return KeyCode.CAPS_LOCK;

        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD1:
            return KeyCode.NUMPAD_1;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD2:
            return KeyCode.NUMPAD_2;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD3:
            return KeyCode.NUMPAD_3;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD4:
            return KeyCode.NUMPAD_4;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD5:
            return KeyCode.NUMPAD_5;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD6:
            return KeyCode.NUMPAD_6;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD7:
            return KeyCode.NUMPAD_7;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD8:
            return KeyCode.NUMPAD_8;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD9:
            return KeyCode.NUMPAD_9;
        case com.jogamp.newt.event.KeyEvent.VK_NUMPAD0:
            return KeyCode.NUMPAD_0;
        case com.jogamp.newt.event.KeyEvent.VK_ADD:
            return KeyCode.NUMPAD_ADD;
        case com.jogamp.newt.event.KeyEvent.VK_SUBTRACT:
            return KeyCode.NUMPAD_SUBTRACT;
        case com.jogamp.newt.event.KeyEvent.VK_DECIMAL:
            return KeyCode.NUMPAD_DECIMAL;
        case com.jogamp.newt.event.KeyEvent.VK_DIVIDE:
            return KeyCode.NUMPAD_DIVIDE;
        case com.jogamp.newt.event.KeyEvent.VK_MULTIPLY:
            return KeyCode.NUMPAD_MULTIPLY;

        case com.jogamp.newt.event.KeyEvent.VK_ALT:
            // as far as I can tell NEWT does not provide locations
            return KeyCode.LEFT_ALT;
        case com.jogamp.newt.event.KeyEvent.VK_CONTROL:
            return KeyCode.LEFT_CONTROL;
        case com.jogamp.newt.event.KeyEvent.VK_SHIFT:
            return KeyCode.LEFT_SHIFT;
        case com.jogamp.newt.event.KeyEvent.VK_META:
            return KeyCode.LEFT_META;
        default:
            return KeyCode.UNKNOWN;
        }
    }

    private char getCharacter(com.jogamp.newt.event.KeyEvent e) {
        if (e.getKeyChar() == com.jogamp.newt.event.KeyEvent.VK_UNDEFINED) {
            return KeyEvent.CHAR_UNKNOWN;
        } else {
            return e.getKeyChar();
        }
    }

    private MouseButton getButton(com.jogamp.newt.event.MouseEvent e) {
        switch (e.getButton()) {
        case 0:
            return MouseButton.NONE;
        case com.jogamp.newt.event.MouseEvent.BUTTON1:
            return MouseButton.LEFT;
        case com.jogamp.newt.event.MouseEvent.BUTTON2:
            return MouseButton.RIGHT;
        case com.jogamp.newt.event.MouseEvent.BUTTON3:
            return MouseButton.CENTER;
        default:
            throw new IllegalArgumentException("Unknown NEWT button code");
        }
    }

    private int getY(com.jogamp.newt.event.MouseEvent e) {
        // NEWT uses the same coordinate space as AWT so we have to flip y values
        return component.getHeight() - e.getY();
    }

    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
        // FIXME NEWT on Mac does not seem to fire of press/release events
        // for modifier keys
        KeyEvent event = new KeyEvent(KeyEvent.Type.PRESS,
                                      dispatcher.getSource(),
                                      getKeyCode(e),
                                      getCharacter(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
        KeyEvent event = new KeyEvent(KeyEvent.Type.RELEASE,
                                      dispatcher.getSource(),
                                      getKeyCode(e),
                                      getCharacter(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void keyTyped(com.jogamp.newt.event.KeyEvent e) {
        // ignore this event
    }

    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.PRESS,
                                          dispatcher.getSource(),
                                          e.getX(),
                                          getY(e),
                                          0,
                                          getButton(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.RELEASE,
                                          dispatcher.getSource(),
                                          e.getX(),
                                          getY(e),
                                          0,
                                          getButton(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
        // we treat a drag just like any other mouse move, the mouse
        // down has already been recorded by a mousePressed() event
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.MOVE,
                                          dispatcher.getSource(),
                                          e.getX(),
                                          getY(e),
                                          0,
                                          MouseButton.NONE);
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.SCROLL,
                                          dispatcher.getSource(),
                                          e.getX(),
                                          getY(e),
                                          e.getWheelRotation(),
                                          MouseButton.NONE);
        dispatcher.dispatchEvent(event);
    }
}

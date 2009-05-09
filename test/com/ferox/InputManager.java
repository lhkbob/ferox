package com.ferox;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;

//FIXME: make this better, cleaner and more interfacy, not permanent
public class InputManager implements KeyListener, MouseListener,
	MouseMotionListener, MouseWheelListener {
	public static final int NORMAL = 0;
	public static final int RELATIVE = 1;
	public static final int INITIAL_PRESS = 1;
	public static final int MOVED = 0;
	public static final int DRAGGED = 1;
	public static final int RELEASED = 0;
	public static final int PRESSED = 1;
	public static final int CLICKED = 2;
	public static final int WAITING_FOR_RELEASE = 2;

	private static final int NUM_KEYS = 600;
	private static final int NUM_MOUSE_BUTTONS = 3;

	public static final Cursor INVISIBLE_CURSOR =
		Toolkit.getDefaultToolkit().createCustomCursor(
			Toolkit.getDefaultToolkit().getImage(""), new Point(0, 0),
			"invisible");

	private final int keyPressed[][];
	private final int mouseButtonPressed[];
	private int mouseMotionType = 0;
	private int mouseWheelChange = 0;
	private int mouseXChange = 0;
	private int mouseYChange = 0;
	private int mouseBehavior;
	private int buttonOnDrag;

	public static final int centerWheel = 2;
	public static final int leftClick = 1;
	public static final int rightClick = 3;

	private final Point mouseLocation;
	private final Point centerLocation;
	private final Point mouseAtButton1[];
	private final Point mouseAtButton2[];
	private final Point mouseAtButton3[];
	private final Point mouseAtWheel;

	private Component comp;
	public Robot robot;

	private boolean isRecentering;

	public InputManager(Component comp) {
		this(comp, NORMAL);
	}

	public InputManager(Component comp, int behavior) {
		mouseLocation = new Point();
		centerLocation = new Point();
		mouseAtButton1 = new Point[3];
		mouseAtButton2 = new Point[3];
		mouseAtButton3 = new Point[3];
		mouseAtWheel = new Point();

		for (int i = 0; i < 3; i++) {
			mouseAtButton1[i] = new Point();
			mouseAtButton2[i] = new Point();
			mouseAtButton3[i] = new Point();
		}

		keyPressed = new int[NUM_KEYS][2];
		mouseButtonPressed = new int[NUM_MOUSE_BUTTONS];
		setInputComponent(comp);
		setBehavior(behavior);
	}

	public void setInputComponent(Component comp) {
		if (this.comp != null) {
			this.comp.removeKeyListener(this);
			this.comp.removeMouseListener(this);
			this.comp.removeMouseMotionListener(this);
			this.comp.removeMouseWheelListener(this);
		}
		this.comp = comp;
		if (this.comp != null) {
			this.comp.addKeyListener(this);
			this.comp.addMouseListener(this);
			this.comp.addMouseMotionListener(this);
			this.comp.addMouseWheelListener(this);
		}
	}

	public void setCursor(Cursor cursor) {
		comp.setCursor(cursor);
	}

	public void setBehavior(int behavior) {
		if (behavior == 0 || behavior == 1) {
			mouseBehavior = behavior;
		} else {
			mouseBehavior = 0;
		}
		if (behavior == 1) {
			setRelativeMouseMode(mouseBehavior);
		}
	}

	private void setRelativeMouseMode(int mode) {
		if (mode == isRelativeMouseMode()) {
			return;
		}
		if (mode == RELATIVE) {
			try {
				robot = new Robot();
				recenterMouse();
				setCursor(INVISIBLE_CURSOR);
			} catch (AWTException ex) {
				robot = null;
			}
		} else {
			robot = null;
		}
	}

	public int isRelativeMouseMode() {
		if (robot == null) {
			return NORMAL;
		} else {
			return RELATIVE;
		}
	}

	private synchronized void recenterMouse() {
		if (robot != null && comp.isShowing()) {
			centerLocation.x = comp.getWidth() / 2;
			centerLocation.y = comp.getHeight() / 2;
			SwingUtilities.convertPointToScreen(centerLocation, comp);
			isRecentering = true;
			robot.mouseMove(centerLocation.x, centerLocation.y);
		}

	}

	public void mouseMoved(MouseEvent e) {
		if (isRecentering && mouseBehavior == 1 && centerLocation.x == e.getX()
			&& centerLocation.y == e.getY()) {
			isRecentering = false;
		} else {
			mouseXChange = (e.getX()) - mouseLocation.x;
			mouseYChange = (e.getY()) - mouseLocation.y;
			if (mouseBehavior == 1) {
				recenterMouse();
			}
			mouseMotionType = MOVED;
		}
		mouseLocation.x = (e.getX());
		mouseLocation.y = (e.getY());
		e.consume();
	}

	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
		mouseMotionType = DRAGGED;
	}

	public void mousePressed(MouseEvent e) {
		int button = 0;

		switch (e.getButton()) {
		case MouseEvent.BUTTON1:
			button = 0;
			mouseAtButton1[0].x = (e.getX());
			mouseAtButton1[0].y = (e.getY());
			break;
		case MouseEvent.BUTTON2:
			button = 1;
			mouseAtButton2[0].x = (e.getX());
			mouseAtButton2[0].y = (e.getY());
			break;
		case MouseEvent.BUTTON3:
			button = 2;
			mouseAtButton3[0].x = (e.getX());
			mouseAtButton3[0].y = (e.getY());
			break;
		default:
			button = 0;
			break;
		}
		mouseButtonPressed[button] = PRESSED;
		buttonOnDrag = e.getButton();
		e.consume();
	}

	public void mouseReleased(MouseEvent e) {
		int button = 0;

		switch (e.getButton()) {
		case MouseEvent.BUTTON1:
			button = 0;
			mouseAtButton1[1].x = (e.getX());
			mouseAtButton1[1].y = (e.getY());
			break;
		case MouseEvent.BUTTON2:
			button = 1;
			mouseAtButton2[1].x = (e.getX());
			mouseAtButton2[1].y = (e.getY());
			break;
		case MouseEvent.BUTTON3:
			button = 2;
			mouseAtButton3[1].x = (e.getX());
			mouseAtButton3[1].y = (e.getY());
			break;
		default:
			button = 0;
			break;
		}
		mouseButtonPressed[button] = RELEASED;
		buttonOnDrag = -1;
		e.consume();
	}

	public void mouseClicked(MouseEvent e) {
		int button = 0;

		switch (e.getButton()) {
		case MouseEvent.BUTTON1:
			button = 0;
			mouseAtButton1[2].x = (e.getX());
			mouseAtButton1[2].y = (e.getY());
			break;
		case MouseEvent.BUTTON2:
			button = 1;
			mouseAtButton2[2].x = (e.getX());
			mouseAtButton2[2].y = (e.getY());
			break;
		case MouseEvent.BUTTON3:
			button = 2;
			mouseAtButton3[2].x = (e.getX());
			mouseAtButton3[2].y = (e.getY());
			break;

		default:
			button = 0;
			break;
		}
		mouseButtonPressed[button] = CLICKED;
		buttonOnDrag = -1;
		e.consume();
	}

	public void mouseEntered(MouseEvent e) {
		mouseMoved(e);
	}

	public void mouseExited(MouseEvent e) {
		mouseMoved(e);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() >= 0 && e.getKeyCode() < 600) {
			if (keyPressed[e.getKeyCode()][0] == RELEASED
				|| keyPressed[e.getKeyCode()][0] != WAITING_FOR_RELEASE) {
				keyPressed[e.getKeyCode()][0] = PRESSED;
			}
		}
		e.consume();
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() >= 0 && e.getKeyCode() < 600) {
			keyPressed[e.getKeyCode()][0] = RELEASED;
		}
		e.consume();
	}

	public void keyTyped(KeyEvent e) {
		e.consume();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		mouseWheelChange = e.getWheelRotation();
		mouseAtWheel.x = (e.getX());// +(int)currentComp.getBounds().getX());//
		mouseAtWheel.y = (e.getY());// )+(int)currentComp.getBounds().getY());
		e.consume();
	}

	public boolean isKeyPressed(int keyCode) {
		if (keyCode >= 0 && keyCode < 600) {
			// return (keyPressed[keyCode][0]==PRESSED);
			if (keyPressed[keyCode][0] == PRESSED
				&& keyPressed[keyCode][1] == NORMAL) {
				return true;
			}
			if (keyPressed[keyCode][0] == PRESSED
				&& keyPressed[keyCode][1] == INITIAL_PRESS) {
				keyPressed[keyCode][0] = WAITING_FOR_RELEASE;
				return true;
			}
			return false;
		} else {
			return false;
		}
	}

	public boolean isMousePressed(int button) {
		button--;
		if (button >= 0 && button < 3) {
			if (mouseButtonPressed[button] == PRESSED) {
				// mouseButtonPressed[button]=RELEASED;
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	public int getLastMouseX() {
		return mouseLocation.x;
	}

	public int getLastMouseY() {
		return mouseLocation.y;
	}

	public int getMouseAtLastButtonPressX(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[0].x;
		case 2:
			return mouseAtButton2[0].x;
		case 3:
			return mouseAtButton3[0].x;
		default:
			return -0;
		}
	}

	public int getMouseAtLastButtonPressY(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[0].y;
		case 2:
			return mouseAtButton2[0].y;
		case 3:
			return mouseAtButton3[0].y;
		default:
			return -0;
		}
	}

	public int getMouseAtLastButtonReleaseX(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[1].x;
		case 2:
			return mouseAtButton2[1].x;
		case 3:
			return mouseAtButton3[1].x;
		default:
			return -0;
		}
	}

	public int getMouseAtLastButtonReleaseY(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[1].y;
		case 2:
			return mouseAtButton2[1].y;
		case 3:
			return mouseAtButton3[1].y;
		default:
			return -0;
		}
	}

	public int getMouseAtLastButtonClickX(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[2].x;
		case 2:
			return mouseAtButton2[2].x;
		case 3:
			return mouseAtButton3[2].x;
		default:
			return -0;
		}
	}

	public int getMouseAtLastButtonClickY(int button) {
		switch (button) {
		case 1:
			return mouseAtButton1[2].y;
		case 2:
			return mouseAtButton2[2].y;
		case 3:
			return mouseAtButton3[2].y;
		default:
			return -0;
		}
	}

	public int getMouseAtLastWheelMoveX() {
		return mouseAtWheel.x;
	}

	public int getMouseAtLastWheelMoveY() {
		return mouseAtWheel.y;
	}

	public int getMouseXChange() {
		int mouseX = mouseXChange;
		mouseXChange = 0;
		if (mouseBehavior == RELATIVE) {
			return -mouseX;
		}
		return mouseX;
	}

	public int getMouseYChange() {
		int mouseY = mouseYChange;
		mouseYChange = 0;
		if (mouseBehavior == RELATIVE) {
			return -mouseY;
		}
		return mouseY;
	}

	public int getMouseMotionType() {
		return mouseMotionType;
	}

	public int getWheelChange() {
		int wheel = mouseWheelChange;
		mouseWheelChange = 0;
		return wheel;
	}

	public int getMouseBehavior() {
		return mouseBehavior;
	}

	public int getKeyBehavior(int keyCode) {
		if (keyCode >= 0 && keyCode < 600) {
			return keyPressed[keyCode][1];
		} else {
			return -1;
		}
	}

	public Component getListeningComponent() {
		return comp;
	}

	public int getButtonOnDrag() {
		return buttonOnDrag;
	}

	public void setKeyBehavior(int keyCode, int behavior) {
		if (keyCode >= 0 && keyCode < 600) {
			if (behavior == 0 || behavior == 1) {
				keyPressed[keyCode][1] = behavior;
			}
		}
	}
}

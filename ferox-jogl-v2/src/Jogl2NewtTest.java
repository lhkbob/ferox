import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.sun.javafx.newt.KeyEvent;
import com.sun.javafx.newt.KeyListener;
import com.sun.javafx.newt.opengl.GLWindow;


public class Jogl2NewtTest implements GLEventListener, KeyListener {
	public static void main(String[] args) throws Exception {
		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		caps.setAlphaBits(8);
		caps.setBlueBits(8);
		caps.setGreenBits(8);
		caps.setRedBits(8);
		
		GLWindow window = GLWindow.create(caps);
		window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE);
		
		Jogl2NewtTest t = new Jogl2NewtTest();
		window.addGLEventListener(t);
		window.addKeyListener(t);
		
		window.setVisible(true);
		for (int i = 0; i < 100000; i++) {
			window.display();
			window.invalidate();
		}
		
		window.destroy();
	}

	public void display(GLAutoDrawable arg0) {
		GL2 gl = arg0.getGL().getGL2();
		gl.glClearColor(0f, 0f, 0f, 1f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	public void dispose(GLAutoDrawable arg0) {
		System.out.println("dispose called on thread: " + Thread.currentThread().getName());
	}

	public void init(GLAutoDrawable arg0) {
		System.out.println("init called on thread: " + Thread.currentThread().getName());
	}

	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("key pressed: " + e.getKeyChar());
	}

	@Override
	public void keyReleased(KeyEvent e) {
		System.out.println("key released: " + e.getKeyChar());
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println("key typed: " + e.getKeyChar());
	}
}

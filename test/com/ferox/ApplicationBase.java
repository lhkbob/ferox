package com.ferox;

import java.awt.Component;
import java.awt.event.KeyEvent;

import com.ferox.math.Transform;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.jogl.BasicJoglRenderer;

/** ApplicationBase provides a common, simple framework for
 * tests to build off of.  It uses the BasicJoglRenderer.
 * 
 * @author Michael Ludwig
 *
 */
public class ApplicationBase {
	public static final float T_VEL = 20f;
	
	private FrameStatistics stats;
	private Renderer renderer;
	
	private InputManager input;
	private Transform toMove;
	
	private boolean exiting;
	
	public ApplicationBase(boolean debug) {
		this.stats = new FrameStatistics();
		this.input = null;
		this.renderer = new BasicJoglRenderer(debug);
	}
	
	/** Init, then update and render at full speed, and then 
	 * clean-up the mess. */
	public void run() {
		this.init(this.renderer);
		
		while(!this.exiting) {
			if (this.input != null)
				this.handleInput(this.input, this.stats.getTotalTime() / 1e9f);
			
			this.exiting = this.exiting || this.update() || this.render(this.renderer);
		}
		
		this.destroy(this.renderer);
		System.exit(0);
	}
	
	/** Configure input handling so that it listens on the given compoent.
	 * If mover isn't null, it will be the transform automatically moved
	 * by the default implementation of handleInput(). */
	protected void configureInputHandling(Component c, Transform mover) {
		this.input = new InputManager(c);
		this.input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
		this.input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);
		
		this.toMove = mover;
	}
	
	/** dt is change in time, in seconds. */
	protected void handleInput(InputManager input, float dt) {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) 
			this.exiting = true;
		if (input.isKeyPressed(KeyEvent.VK_R))
			this.stats.reportStatistics(System.out);
		
		if (this.toMove != null) {
			float t = dt * T_VEL;
			
			if (input.isKeyPressed(KeyEvent.VK_LEFT))
				this.toMove.getTranslation().x += -t;
			if (input.isKeyPressed(KeyEvent.VK_RIGHT))
				this.toMove.getTranslation().x += t;
			if (input.isKeyPressed(KeyEvent.VK_S))
				this.toMove.getTranslation().y += -t;
			if (input.isKeyPressed(KeyEvent.VK_W))
				this.toMove.getTranslation().y += t;
			if (input.isKeyPressed(KeyEvent.VK_UP))
				this.toMove.getTranslation().z += t;
			if (input.isKeyPressed(KeyEvent.VK_DOWN))
				this.toMove.getTranslation().z += -t;
		}
	}
	
	/** Perform initial setup with the given renderer,
	 * which will be used later on. */
	protected void init(Renderer renderer) {
		// do nothing
	}
	
	/** Update any scene elements, etc. before the rendering
	 * will be started.  Return true if the application should
	 * exit. */
	protected boolean update() {
		// do nothing
		return false;
	}
	
	/** Render everything.  This implementation just calls
	 * flushRenderer() and returns false.  Subclasses should
	 * queue surfaces before calling super().
	 * 
	 * Return true if the application should exit. */
	protected boolean render(Renderer renderer) {
		renderer.flushRenderer(this.stats);
		return false;
	}
	
	/** Clean-up things.  This implementation calls
	 * destroy() on the given renderer, so overridden
	 * methods must call super() at the end. */
	protected void destroy(Renderer renderer) {
		renderer.destroy();
	}
}

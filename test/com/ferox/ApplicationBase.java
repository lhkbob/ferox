package com.ferox;

import java.awt.Component;
import java.awt.event.KeyEvent;

import com.ferox.math.Transform;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.jogl.BasicJoglRenderer;

/**
 * ApplicationBase provides a common, simple framework for tests to build off
 * of. It uses the BasicJoglRenderer.
 * 
 * @author Michael Ludwig
 */
public class ApplicationBase {
	public static final float T_VEL = 40f;

	protected FrameStatistics stats;
	private final Renderer renderer;

	private InputManager input;
	private Transform toMove;

	private boolean exiting;

	public ApplicationBase(boolean debug) {
		stats = new FrameStatistics();
		input = null;
		renderer = new BasicJoglRenderer(debug);
	}

	/**
	 * Init, then update and render at full speed, and then clean-up the mess.
	 */
	public void run() {
		init(renderer);

		while (!exiting) {
			if (input != null) {
				handleInput(input, stats.getTotalTime() / 1e9f);
			}

			exiting = exiting || update() || render(renderer);
		}

		destroy(renderer);
		System.exit(0);
	}

	/**
	 * Configure input handling so that it listens on the given compoent. If
	 * mover isn't null, it will be the transform automatically moved by the
	 * default implementation of handleInput().
	 */
	protected void configureInputHandling(Component c, Transform mover) {
		input = new InputManager(c);
		input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
		input.setKeyBehavior(KeyEvent.VK_M, InputManager.INITIAL_PRESS);
		input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);

		toMove = mover;
	}

	/** dt is change in time, in seconds. */
	protected void handleInput(InputManager input, float dt) {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) {
			exiting = true;
		}
		if (input.isKeyPressed(KeyEvent.VK_R)) {
			stats.reportStatistics(System.out);
		}
		if (input.isKeyPressed(KeyEvent.VK_M)) {
			Runtime run = Runtime.getRuntime();
			System.out.printf(
				"Memory Usage | Heap Size: %.2f M Free: %.2f M\n", run
					.totalMemory() / 1e6f, run.freeMemory() / 1e6f);
		}

		if (toMove != null) {
			float t = dt * T_VEL;

			if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
				toMove.getTranslation().x += -t;
			}
			if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
				toMove.getTranslation().x += t;
			}
			if (input.isKeyPressed(KeyEvent.VK_S)) {
				toMove.getTranslation().y += -t;
			}
			if (input.isKeyPressed(KeyEvent.VK_W)) {
				toMove.getTranslation().y += t;
			}
			if (input.isKeyPressed(KeyEvent.VK_UP)) {
				toMove.getTranslation().z += t;
			}
			if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
				toMove.getTranslation().z += -t;
			}
		}
	}

	/**
	 * Perform initial setup with the given renderer, which will be used later
	 * on.
	 */
	protected void init(Renderer renderer) {
		// do nothing
	}

	/**
	 * Update any scene elements, etc. before the rendering will be started.
	 * Return true if the application should exit.
	 */
	protected boolean update() {
		// do nothing
		return false;
	}

	/**
	 * Render everything. This implementation just calls flushRenderer() and
	 * returns false. Subclasses should queue surfaces before calling super().
	 * Return true if the application should exit.
	 */
	protected boolean render(Renderer renderer) {
		renderer.flushRenderer(stats);
		return false;
	}

	/**
	 * Clean-up things. This implementation calls destroy() on the given
	 * renderer, so overridden methods must call super() at the end.
	 */
	protected void destroy(Renderer renderer) {
		renderer.destroy();
	}
}

package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.DisplayOptions.AntiAliasMode;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.sun.javafx.newt.WindowEvent;
import com.sun.javafx.newt.WindowListener;
import com.sun.javafx.newt.opengl.GLWindow;

/**
 * Abstract class that provides the majority of the implementation of Jogl's
 * WindowSurface and FullscreenSurface. It uses Newt's GLWindow native window
 * implementation. Every JoglOnscreenSurface gets its own state record.
 * 
 * @author Michael Ludwig
 */
public abstract class JoglOnscreenSurface extends JoglRenderSurface implements OnscreenSurface, WindowListener {
	protected final GLWindow window;

	private final JoglStateRecord record;

	private DisplayOptions options;

	private boolean enableVsync;
	private boolean updateVsync;

	/**
	 * The given options are used to identify the GLCapabilities for the
	 * constructed GLCanvas. The GLCanvas shares with the given factory's shadow
	 * context.
	 */
	public JoglOnscreenSurface(JoglContextManager factory, GLProfile profile, DisplayOptions optionsRequest, 
							   int x, int y, int width, int height, 
							   boolean resizable, boolean undecorated) {
		super(factory);
		if (optionsRequest == null)
			optionsRequest = new DisplayOptions();
		
		GLCapabilities glCaps = chooseCapabilities(profile, optionsRequest);
		window = GLWindow.create(glCaps, undecorated);
		window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE);
		
		window.setSize(Math.max(width, 1), Math.max(height, 1));
		window.setPosition(x, y);

		window.setVisible(true);
		// must create context after its visible
		GLContext old = window.getContext();
		GLContext shared = window.createContext(factory.getShadowContext());
		window.setContext(shared);
		old.destroy(); // clean-up
		
		window.addWindowListener(this);
		window.addGLEventListener(this);

		record = new JoglStateRecord(factory.getFramework().getCapabilities());
		options = optionsRequest;

		enableVsync = false;
		updateVsync = true;
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return window;
	}

	/** In addition, destroys the context of this surface's GLWindow. */
	@Override
	public void destroySurface() {
		window.removeWindowListener(this);
		window.destroy();
		super.destroySurface();
	}

	/**
	 * Overridden to apply any vsync changes and to detect the actual
	 * DisplayOptions the first time the surface is made current.
	 */
	@Override
	public void preRenderAction() {
		GL gl = factory.getGL();

		if (updateVsync) {
			gl.setSwapInterval(enableVsync ? 1 : 0);
			updateVsync = false;
		}
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return record;
	}

	@Override
	public void init() {
		// fetch the detected options
		options = detectOptions(factory.getGL());
	}

	@Override
	public void postRenderAction(JoglRenderSurface next) {
		// do nothing
	}

	@Override
	public Object getWindowImpl() {
		return window;
	}

	@Override
	public boolean isVSyncEnabled() {
		return enableVsync;
	}

	@Override
	public void setVSyncEnabled(boolean enable) {
		enableVsync = enable;
		updateVsync = true;
	}

	@Override
	public DisplayOptions getDisplayOptions() {
		return options;
	}

	@Override
	public int getHeight() {
		return window.getHeight();
	}

	@Override
	public int getWidth() {
		return window.getWidth();
	}

	@Override
	public String getTitle() {
		return window.getTitle();
	}

	@Override
	public void setTitle(String title) {
		window.setTitle(title);
	}

	@Override
	public boolean isVisible() {
		return !isDestroyed() && window.isVisible();
	}

	/* Unfortunate consequences of being a window listener. */
	@Override
	public void windowDestroyNotify(WindowEvent e) {
		factory.destroy(this);
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
	}

	@Override
	public void windowMoved(WindowEvent e) {
	}

	@Override
	public void windowResized(WindowEvent e) {
	}

	/* Utility methods. */

	private static GLCapabilities chooseCapabilities(GLProfile profile, DisplayOptions request) {
		GLCapabilities caps = new GLCapabilities(profile);
		// try to update the caps fields
		switch (request.getPixelFormat()) {
		case RGB_16BIT:
			caps.setRedBits(5);
			caps.setGreenBits(6);
			caps.setBlueBits(5);
			caps.setAlphaBits(0);
			break;
		case RGB_24BIT:
		case RGB_FLOAT:
		case NONE:
			caps.setRedBits(8);
			caps.setGreenBits(8);
			caps.setBlueBits(8);
			caps.setAlphaBits(0);
			break;
		case RGBA_32BIT:
		case RGBA_FLOAT:
			caps.setRedBits(8);
			caps.setGreenBits(8);
			caps.setBlueBits(8);
			caps.setAlphaBits(8);
			break;
		}

		switch (request.getDepthFormat()) {
		case DEPTH_16BIT:
			caps.setDepthBits(16);
			break;
		case DEPTH_24BIT:
			caps.setDepthBits(24);
			break;
		case DEPTH_32BIT:
			caps.setDepthBits(32);
			break;
		case NONE:
			caps.setDepthBits(0);
			break;
		}

		switch (request.getStencilFormat()) {
		case STENCIL_16BIT:
			caps.setStencilBits(16);
			break;
		case STENCIL_8BIT:
			caps.setStencilBits(8);
			break;
		case STENCIL_4BIT:
			caps.setStencilBits(4);
			break;
		case STENCIL_1BIT:
			caps.setStencilBits(1);
			break;
		case NONE:
			caps.setStencilBits(0);
			break;
		}

		switch (request.getAntiAliasing()) {
		case EIGHT_X:
			caps.setNumSamples(8);
			caps.setSampleBuffers(true);
			break;
		case FOUR_X:
			caps.setNumSamples(4);
			caps.setSampleBuffers(true);
			break;
		case TWO_X:
			caps.setNumSamples(2);
			caps.setSampleBuffers(true);
			break;
		case NONE:
			caps.setNumSamples(0);
			caps.setSampleBuffers(false);
			break;
		}

		return caps;
	}

	private static DisplayOptions detectOptions(GL gl) {
		int[] t = new int[1];
		int red, green, blue, alpha, stencil, depth;
		int samples, sampleBuffers;

		gl.glGetIntegerv(GL.GL_RED_BITS, t, 0);
		red = t[0];
		gl.glGetIntegerv(GL.GL_GREEN_BITS, t, 0);
		green = t[0];
		gl.glGetIntegerv(GL.GL_BLUE_BITS, t, 0);
		blue = t[0];
		gl.glGetIntegerv(GL.GL_ALPHA_BITS, t, 0);
		alpha = t[0];

		gl.glGetIntegerv(GL.GL_STENCIL_BITS, t, 0);
		stencil = t[0];
		gl.glGetIntegerv(GL.GL_DEPTH_BITS, t, 0);
		depth = t[0];

		gl.glGetIntegerv(GL.GL_SAMPLES, t, 0);
		samples = t[0];
		gl.glGetIntegerv(GL.GL_SAMPLE_BUFFERS, t, 0);
		sampleBuffers = t[0];

		PixelFormat format = PixelFormat.RGB_24BIT;
		switch (red + green + blue + alpha) {
		case 32:
			format = PixelFormat.RGBA_32BIT;
			break;
		case 24:
			format = PixelFormat.RGB_24BIT;
			break;
		case 16:
			format = PixelFormat.RGB_16BIT;
			break;
		}

		DepthFormat df = DepthFormat.NONE;
		switch (depth) {
		case 16:
			df = DepthFormat.DEPTH_16BIT;
			break;
		case 24:
			df = DepthFormat.DEPTH_24BIT;
			break;
		case 32:
			df = DepthFormat.DEPTH_32BIT;
			break;
		}

		StencilFormat sf = StencilFormat.NONE;
		switch (stencil) {
		case 16:
			sf = StencilFormat.STENCIL_16BIT;
			break;
		case 8:
			sf = StencilFormat.STENCIL_8BIT;
			break;
		case 4:
			sf = StencilFormat.STENCIL_4BIT;
			break;
		case 1:
			sf = StencilFormat.STENCIL_1BIT;
			break;
		}

		AntiAliasMode aa = AntiAliasMode.NONE;
		if (sampleBuffers != 0) {
			switch (samples) {
			case 8:
				aa = AntiAliasMode.EIGHT_X;
				break;
			case 4:
				aa = AntiAliasMode.FOUR_X;
				break;
			case 2:
				aa = AntiAliasMode.TWO_X;
				break;
			}

			gl.glEnable(GL2.GL_MULTISAMPLE);
		} else
			gl.glDisable(GL2.GL_MULTISAMPLE);

		return new DisplayOptions(format, df, sf, aa);
	}
}

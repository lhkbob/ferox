package com.ferox.renderer.impl.jogl;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.swing.SwingUtilities;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.DisplayOptions.AntiAliasMode;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/** Abstract class that provides the majority of the implementation of 
 * Jogl's WindowSurface and FullscreenSurface.  It assumes the use of a GLCanvas
 * and a Frame for the representation of the surface.
 * 
 * Every JoglOnscreenSurface gets its own state record.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class JoglOnscreenSurface extends JoglRenderSurface implements OnscreenSurface, WindowListener {
	protected final GLCanvas canvas;
	protected Frame frame; // final
	
	private final JoglStateRecord record;
	
	private DisplayOptions options;
	
	private boolean iconified;
	
	private boolean enableVsync;
	private boolean updateVsync;
	
	/** The given options is used to identify the GLCapabilities for the
	 * constructed GLCanvas.  The GLCanvas shares with the given factory's 
	 * shadow context. */
	protected JoglOnscreenSurface(JoglSurfaceFactory factory, int id, DisplayOptions optionsRequest, 
								  final int x, final int y, final int width, final int height, 
								  final boolean resizable, final  boolean undecorated) {
		super(factory, id);
		if (optionsRequest == null)
			optionsRequest = new DisplayOptions();
		this.canvas = new GLCanvas(chooseCapabilities(optionsRequest), new DefaultGLCapabilitiesChooser(), factory.getShadowContext(), null);
		this.frame = new Frame();

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					frame.setResizable(resizable);
					frame.setUndecorated(undecorated);
					frame.setBounds(x, y, Math.max(width, 1), Math.max(height, 1));

					frame.add(canvas);

					frame.setVisible(true);
					canvas.requestFocusInWindow();
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error creating JoglOnscreenSurface", e);
		}

		this.frame.addWindowListener(this);
		
		this.canvas.addGLEventListener(this);
		this.canvas.setIgnoreRepaint(true);
		
		this.record = new JoglStateRecord(factory.getRenderer().getCapabilities());
		this.options = optionsRequest;
		
		this.enableVsync = false;
		this.updateVsync = true;
		this.iconified = false;
	}
		
	/** Return the gl canvas that must be the sole child of the frame
	 * returned by getFrame(). */
	@Override
	public GLCanvas getGLAutoDrawable() {
		return this.canvas;
	}
	
	/** In addition, destroys the context of this surface's GLCanvas. */
	@Override
	public void destroySurface() {
		this.frame.removeWindowListener(JoglOnscreenSurface.this);

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					frame.setVisible(false);
					frame.dispose();
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error hiding JoglWindowSurface", e);
		}
		
		this.canvas.getContext().destroy();
		super.destroySurface();
	}
	
	/** Overridden to apply any vsync changes and to detect the actual DisplayOptions
	 * the first time the surface is made current. */
	@Override
	public void preRenderAction() {
		GL gl = this.factory.getGL();
		
		if (this.updateVsync) {
			if (this.enableVsync)
				gl.setSwapInterval(1);
			else
				gl.setSwapInterval(0);
			this.updateVsync = false;
		}
	}
	
	@Override
	public JoglStateRecord getStateRecord() {
		return this.record;
	}

	@Override
	public void init() {
		// fetch the detected options
		this.options = detectOptions(this.factory.getGL());
	}

	@Override
	public void postRenderAction(JoglRenderSurface next) {
		// do nothing
	}

	@Override
	public Object getWindowImpl() {
		return this.frame;
	}
	
	@Override
	public boolean isVSyncEnabled() {
		return this.enableVsync;
	}
	
	@Override
	public void setVSyncEnabled(boolean enable) {
		this.enableVsync = enable;
		this.updateVsync = true;
	}

	@Override
	public DisplayOptions getDisplayOptions() {
		return this.options;
	}

	@Override
	public int getHeight() {
		return this.canvas.getHeight();
	}

	@Override
	public int getWidth() {
		return this.canvas.getWidth();
	}
	
	@Override
	public String getTitle() {
		return this.frame.getTitle();
	}

	@Override
	public void setTitle(final String title) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JoglOnscreenSurface.this.frame.setTitle(title == null ? "" : title);
			}
		});
	}
	
	@Override
	public boolean isVisible() {
		return !this.isDestroyed() && !this.iconified;
	}
	
	/** Do a half-cleanup and notify the factory that we've been destroyed. */
	@Override
	public void windowClosed(WindowEvent e) { }

	/* Unfortunate consequences of being a window listener. */
	
	@Override
	public void windowActivated(WindowEvent e) { }
	
	@Override
	public void windowClosing(WindowEvent e) {
		 // the factory will make sure everything is destroyed properly on the correct thread
		this.factory.notifyOnscreenSurfaceZombie(this);
	}

	@Override
	public void windowDeactivated(WindowEvent e) { }

	@Override
	public void windowDeiconified(WindowEvent e) { 
		this.iconified = false;
	}

	@Override
	public void windowIconified(WindowEvent e) { 
		this.iconified = true;
	}

	@Override
	public void windowOpened(WindowEvent e) { }
	
	/* Utility methods. */
	
	private static GLCapabilities chooseCapabilities(DisplayOptions request) {
		GLCapabilities caps = new GLCapabilities();
		
		// try to update the caps fields
		switch(request.getPixelFormat()) {
		case RGB_16BIT:
			caps.setRedBits(5); caps.setGreenBits(6); caps.setBlueBits(5); caps.setAlphaBits(0); break;
		case RGB_24BIT: case RGB_FLOAT: case NONE:
			caps.setRedBits(8); caps.setGreenBits(8); caps.setBlueBits(8); caps.setAlphaBits(0); break;
		case RGBA_32BIT: case RGBA_FLOAT:
			caps.setRedBits(8); caps.setGreenBits(8); caps.setBlueBits(8); caps.setAlphaBits(8); break;
		}

		switch(request.getDepthFormat()) {
		case DEPTH_16BIT:
			caps.setDepthBits(16); break;
		case DEPTH_24BIT:
			caps.setDepthBits(24); break;
		case DEPTH_32BIT:
			caps.setDepthBits(32); break;
		case NONE:
			caps.setDepthBits(0); break;
		}

		switch(request.getStencilFormat()) {
		case STENCIL_16BIT: 
			caps.setStencilBits(16); break;
		case STENCIL_8BIT:
			caps.setStencilBits(8); break;
		case STENCIL_4BIT:
			caps.setStencilBits(4); break;
		case STENCIL_1BIT: 
			caps.setStencilBits(1); break;
		case NONE:
			caps.setStencilBits(0); break;
		}

		switch(request.getAntiAliasing()) {
		case EIGHT_X:
			caps.setNumSamples(8); caps.setSampleBuffers(true); break;
		case FOUR_X:
			caps.setNumSamples(4); caps.setSampleBuffers(true); break;
		case TWO_X:
			caps.setNumSamples(2); caps.setSampleBuffers(true); break;
		case NONE:
			caps.setNumSamples(0); caps.setSampleBuffers(false); break;
		}

		return caps;
	}
	
	private static DisplayOptions detectOptions(GL gl) {
		int[] t = new int[1];
		int red, green, blue, alpha, stencil, depth;
		int samples, sampleBuffers;
		
		gl.glGetIntegerv(GL.GL_RED_BITS, t, 0); red = t[0];
		gl.glGetIntegerv(GL.GL_GREEN_BITS, t, 0); green = t[0];
		gl.glGetIntegerv(GL.GL_BLUE_BITS, t, 0); blue = t[0];
		gl.glGetIntegerv(GL.GL_ALPHA_BITS, t, 0); alpha = t[0];
		
		gl.glGetIntegerv(GL.GL_STENCIL_BITS, t, 0); stencil = t[0];
		gl.glGetIntegerv(GL.GL_DEPTH_BITS, t, 0); depth = t[0];
		
		gl.glGetIntegerv(GL.GL_SAMPLES, t, 0); samples = t[0];
		gl.glGetIntegerv(GL.GL_SAMPLE_BUFFERS, t, 0); sampleBuffers = t[0];
		
		PixelFormat format = PixelFormat.RGB_24BIT;
		switch(red + green + blue + alpha) {
		case 32:
			format = PixelFormat.RGBA_32BIT; break;
		case 24:
			format = PixelFormat.RGB_24BIT; break;
		case 16:
			format = PixelFormat.RGB_16BIT; break;
		}
		
		DepthFormat df = DepthFormat.NONE;
		switch(depth) {
		case 16: 
			df = DepthFormat.DEPTH_16BIT; break;
		case 24:
			df = DepthFormat.DEPTH_24BIT; break;
		case 32:
			df = DepthFormat.DEPTH_32BIT; break;
		}
		
		StencilFormat sf = StencilFormat.NONE;
		switch(stencil) {
		case 16:
			sf = StencilFormat.STENCIL_16BIT; break;
		case 8:
			sf = StencilFormat.STENCIL_8BIT; break;
		case 4:
			sf = StencilFormat.STENCIL_4BIT; break;
		case 1:
			sf = StencilFormat.STENCIL_1BIT; break;
		}
		
		AntiAliasMode aa = AntiAliasMode.NONE;
		if (sampleBuffers != 0) {
			switch(samples) {
			case 8:
				aa = AntiAliasMode.EIGHT_X; break;
			case 4:
				aa = AntiAliasMode.FOUR_X; break;
			case 2:
				aa = AntiAliasMode.TWO_X; break;
			}
			
			gl.glEnable(GL.GL_MULTISAMPLE);
		} else
			gl.glDisable(GL.GL_MULTISAMPLE);
		
		return new DisplayOptions(format, df, sf, aa);
	}
}

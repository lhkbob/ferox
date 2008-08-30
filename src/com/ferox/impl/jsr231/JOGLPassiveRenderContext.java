package com.ferox.impl.jsr231;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.system.RenderSurface;
import com.ferox.core.util.FeroxException;

public class JOGLPassiveRenderContext extends JOGLRenderContext implements GLEventListener {
	private static final GLU glu = new GLU();
	
	private GL gl;
	private GLContext context;
	private GLAutoDrawable drawer;
	private int version;
	
	private int width;
	private int height;
	
	private int maxDrawBuffers;
	private int numAuxBuffers;

	private boolean initialized;
	
	public JOGLPassiveRenderContext(DisplayOptions options) {
		super(options);
		
		this.width = options.getWidth();
		this.height = options.getHeight();
		
		this.gl = null;
		this.version = -1;
		
		this.maxDrawBuffers = -1;
		this.numAuxBuffers = -1;

		this.initialized = false;
		
		RenderSurface s = this.getRenderSurface();
		if (s instanceof OnscreenRenderSurface) {
			this.drawer = (GLAutoDrawable)((OnscreenRenderSurface)s).getRenderSurface();
		} else {
			this.drawer = ((PbufferSurface)s).getGLPbuffer();
		}
		this.drawer.addGLEventListener(this);
	}

	@Override
	public GL getGL() {
		return this.gl;
	}

	@Override
	public GLContext getGLContext() {
		return this.context;
	}

	@Override
	public int getContextVersion() {
		return this.version;
	}

	@Override
	public int getMaxDrawBuffers() {
		return this.maxDrawBuffers;
	}

	@Override
	public int getNumAuxiliaryBuffers() {
		return this.numAuxBuffers;
	}

	@Override
	public int getContextHeight() {
		return this.height;
	}

	@Override
	public int getContextWidth() {
		return this.width;
	}

	@Override
	public boolean isCurrent() {
		return Threading.isOpenGLThread();
	}

	@Override
	public boolean isInitialized() {
		return this.initialized;
	}

	@Override
	public void render() {
		this.drawer.display();
	}

	public void display(GLAutoDrawable glAD) {		
		this.getRenderManager().notifyRenderFrame();
		
		int error = this.gl.glGetError();
		if (error != 0)
			throw new FeroxException(glu.gluErrorString(error));
		
		this.gl.glFlush();
	}

	public void displayChanged(GLAutoDrawable glAD, boolean arg1, boolean arg2) {
		// do nothing
	}

	public void init(GLAutoDrawable glAD) {
		if (joglCaps == null) {
			JOGLCapabilitiesFetcher fetch = new JOGLCapabilitiesFetcher();
			fetch.init(glAD); // semi-hack of the GLEventListener interface, but in this case it should be okay
			joglCaps = fetch.getCapabilities();
		}
		
		this.version++;
		this.gl = glAD.getGL();
		
		int numMultiSamples = 0;
		boolean doubleBuffered, stereo;
		
		int[] t = new int[1];
		this.gl.glGetIntegerv(GL.GL_MAX_DRAW_BUFFERS, t, 0);
		this.maxDrawBuffers = t[0];
		this.gl.glGetIntegerv(GL.GL_SAMPLES, t, 0);
		numMultiSamples = t[0];
		this.gl.glGetIntegerv(GL.GL_AUX_BUFFERS, t, 0);
		this.numAuxBuffers = t[0];
		
		this.gl.glDrawBuffer(GL.GL_BACK_LEFT);
		doubleBuffered = this.gl.glGetError() == 0;
		
		this.gl.glDrawBuffer(GL.GL_FRONT_RIGHT);
		stereo = this.gl.glGetError() == 0;
		
		if (doubleBuffered)
			this.gl.glDrawBuffer(GL.GL_BACK);
		else
			this.gl.glDrawBuffer(GL.GL_FRONT);
		
		this.gl.glEnable(GL.GL_DEPTH_TEST);
		this.gl.glGetIntegerv(GL.GL_SAMPLE_BUFFERS, t, 0);
		if (t[0] > 0)
			this.gl.glEnable(GL.GL_MULTISAMPLE);
		this.gl.glEnable(GL.GL_SCISSOR_TEST);
		this.gl.glEnable(GL.GL_RESCALE_NORMAL);
		this.gl.glShadeModel(GL.GL_FLAT);
		
		int red, green, blue, alpha, stencil, depth;
		this.gl.glGetIntegerv(GL.GL_RED_BITS, t, 0);
		red = t[0];
		this.gl.glGetIntegerv(GL.GL_GREEN_BITS, t, 0);
		green = t[0];
		this.gl.glGetIntegerv(GL.GL_BLUE_BITS, t, 0);
		blue = t[0];
		this.gl.glGetIntegerv(GL.GL_ALPHA_BITS, t, 0);
		alpha = t[0];
		this.gl.glGetIntegerv(GL.GL_STENCIL_BITS, t, 0);
		stencil = t[0];
		this.gl.glGetIntegerv(GL.GL_DEPTH_BITS, t, 0);
		depth = t[0];
		
		this.gl.glHint(GL.GL_TEXTURE_COMPRESSION_HINT, GL.GL_FASTEST);
		
		RenderSurface s = this.getRenderSurface();
		if (s instanceof GLCanvasSurface) {
			((GLCanvasSurface)s).params = DisplayOptions.createReadOnly(red, green, blue, alpha, depth, stencil, numMultiSamples, stereo, doubleBuffered, false, true);
		} else if (s instanceof GLJPanelSurface) {
			((GLJPanelSurface)s).params = DisplayOptions.createReadOnly(red, green, blue, alpha, depth, stencil, numMultiSamples, stereo, doubleBuffered, false, false);
		} else if (s instanceof PbufferSurface) {
			((PbufferSurface)s).params = DisplayOptions.createReadOnly(red, green, blue, alpha, depth, stencil, numMultiSamples, stereo, doubleBuffered, true, true);
		}
		
		//this.gl.setSwapInterval(1);
		this.gl = new DebugGL(this.gl);
		this.initialized = true;
		this.context = this.drawer.getContext();
		this.getRenderManager().notifyInitialization();
	}

	public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		
		this.getRenderManager().notifyReshape();
	}
}

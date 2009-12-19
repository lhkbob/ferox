package com.ferox.renderer.impl.jogl;

import java.awt.Graphics;
import java.awt.GraphicsDevice;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLCanvas;

public class PaintDisabledGLCanvas extends GLCanvas {
	private static final long serialVersionUID = 1L;

	public PaintDisabledGLCanvas() {
		super();
	}

	public PaintDisabledGLCanvas(GLCapabilities capabilities) {
		super(capabilities);
	}

	public PaintDisabledGLCanvas(GLCapabilities capabilities, GLCapabilitiesChooser chooser,
								 GLContext shareWith, GraphicsDevice device) {
		super(capabilities, chooser, shareWith, device);
	}
	
	@Override
	public void paint(Graphics g) {
		// do nothing, DO NOT call super.paint() since that invokes display
	}
}

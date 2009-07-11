package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL2ES2;

import com.ferox.renderer.impl.jogl.JoglContextManager;

public interface DriverProfile<G extends GL2ES2> {
	public G convert(GL2ES2 base);
	
	public G getGL(JoglContextManager context);
}

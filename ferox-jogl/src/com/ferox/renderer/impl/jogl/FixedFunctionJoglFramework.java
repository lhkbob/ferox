package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;

import com.ferox.renderer.Renderer;

public class FixedFunctionJoglFramework extends JoglFramework {
	public FixedFunctionJoglFramework() {
		super(GLProfile.get(GLProfile.GL2));
	}

	@Override
	protected Renderer createRenderer(JoglContext context) {
		return new FixedFunctionRenderer(this, context);
	}
}

package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.FixedFunctionRendererImpl;

public class FixedFunctionJoglFramework extends JoglFramework {
	private static final int FORCE_BITS = RenderCapabilitiesDetector.FORCE_NO_GLSL;
	
	public FixedFunctionJoglFramework() {
		this(true);
	}
	
	public FixedFunctionJoglFramework(boolean serializeRenders) {
		super(GLProfile.get(GLProfile.GL2), FORCE_BITS, serializeRenders);
	}

	@Override
	protected Renderer createRenderer(JoglContext context) {
		JoglFixedFunctionDelegate ffp = new JoglFixedFunctionDelegate(context, this);
		JoglRendererDelegate core = new JoglRendererDelegate(context);
		return new FixedFunctionRendererImpl(core, ffp);
	}
}

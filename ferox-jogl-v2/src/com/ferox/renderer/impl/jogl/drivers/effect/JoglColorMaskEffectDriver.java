package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL2ES2;

import com.ferox.effect.ColorMask;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * JoglColorMaskEffectDriver provides an EffectDriver implementation that works
 * with instances of ColorMask.
 * 
 * @author Michael Ludwig
 */
public class JoglColorMaskEffectDriver extends SingleEffectDriver<ColorMask, GL2ES2> {

	public JoglColorMaskEffectDriver(JoglContextManager factory) {
		super(new ColorMask(), ColorMask.class, factory);
	}
	
	@Override
	public GL2ES2 convert(GL2ES2 base) {
		return base;
	}

	@Override
	protected void apply(GL2ES2 gl, JoglStateRecord record, ColorMask nextState) {
		boolean[] masks = record.frameRecord.colorWriteMask;
		if (masks[0] != nextState.isRedMasked() || masks[1] != nextState.isGreenMasked() || 
			masks[2] != nextState.isBlueMasked() || masks[3] != nextState.isAlphaMasked()) {
			// assign back to the record
			masks[0] = nextState.isRedMasked();
			masks[1] = nextState.isGreenMasked();
			masks[2] = nextState.isBlueMasked();
			masks[3] = nextState.isRedMasked();
			// execute gl call
			gl.glColorMask(masks[0], masks[1], masks[2], masks[3]);
		}
	}
}

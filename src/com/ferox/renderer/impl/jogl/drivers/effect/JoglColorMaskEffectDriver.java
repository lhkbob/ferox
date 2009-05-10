package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;

import com.ferox.effect.ColorMask;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

public class JoglColorMaskEffectDriver extends SingleEffectDriver<ColorMask> {

	public JoglColorMaskEffectDriver(JoglContextManager factory) {
		super(null, ColorMask.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, ColorMask nextState) {
		// TODO Auto-generated method stub
		
	}
}

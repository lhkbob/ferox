package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

@EffectType(Type.COLOR_MASK)
public class ColorMask extends AbstractEffect {
	private boolean maskRed;
	private boolean maskGreen;
	private boolean maskBlue;
	private boolean maskAlpha;
}

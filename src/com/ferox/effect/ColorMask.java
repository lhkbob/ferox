package com.ferox.effect;

import com.ferox.effect.Effect.Type;

/**
 * <p>
 * The ColorMask Effect controls writing of specific color components to the
 * framebuffer (i.e. the pixel block holding a rendered frame). Each color
 * component: red, green, blue, and alpha, have a boolean mask value. If this
 * value is set to true, color values for that component will be written into
 * the framebuffer. If the value if false, that color component will be
 * effectively masked out, preserving whatever color component was already written
 * to the framebuffer previously.
 * </p>
 * <p>
 * If all components have a mask of true, all color component values are
 * written. If all boolean masks are false, then no color values are written.
 * The default state if no ColorMask is provided is all mask values set to true.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Type(EffectType.COLOR_MASK)
public class ColorMask extends AbstractEffect {
	private boolean maskRed;
	private boolean maskGreen;
	private boolean maskBlue;
	private boolean maskAlpha;

	/**
	 * Construct a ColorMask that allows all color components to be rendered.
	 */
	public ColorMask() {
		this(true, true, true, true);
	}

	/**
	 * Construct a ColorMask with the initial masked values. These are passed
	 * directly to setMasks().
	 * 
	 * @param red The red mask
	 * @param green The green mask
	 * @param blue The blue mask
	 * @param alpha The alpha mask
	 */
	public ColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		setMasks(red, green, blue, alpha);
	}

	/**
	 * Get the red mask state.
	 * 
	 * @return True if red color values are written
	 */
	public boolean isRedMasked() {
		return maskRed;
	}

	/**
	 * Set the red mask value to use when writing color values into the
	 * framebuffer. A value of true allows the red component to be written;
	 * false blocks it.
	 * 
	 * @param maskRed The mask parameter for red
	 */
	public void setRedMasked(boolean maskRed) {
		this.maskRed = maskRed;
	}

	/**
	 * Get the green mask state.
	 * 
	 * @return True if green color values are written
	 */
	public boolean isGreenMasked() {
		return maskGreen;
	}

	/**
	 * Set the green mask value to use when writing color values into the
	 * framebuffer. A value of true allows the green component to be written;
	 * false blocks it.
	 * 
	 * @param maskGreen The mask parameter for green
	 */
	public void setGreenMasked(boolean maskGreen) {
		this.maskGreen = maskGreen;
	}

	/**
	 * Get the blue mask state.
	 * 
	 * @return True if blue color values are written
	 */
	public boolean isBlueMasked() {
		return maskBlue;
	}

	/**
	 * Set the blue mask value to use when writing color values into the
	 * framebuffer. A value of true allows the blue component to be written;
	 * false blocks it.
	 * 
	 * @param maskBlue The mask parameter for blue
	 */
	public void setBlueMasked(boolean maskBlue) {
		this.maskBlue = maskBlue;
	}

	/**
	 * Get the alpha mask state.
	 * 
	 * @return True if alpha color values are written
	 */
	public boolean isAlphaMasked() {
		return maskAlpha;
	}

	/**
	 * Set the alpha mask value to use when writing color values into the
	 * framebuffer. A value of true allows the alpha component to be written;
	 * false blocks it.
	 * 
	 * @param maskAlpha The mask parameter for alpha
	 */
	public void setAlphaMasked(boolean maskAlpha) {
		this.maskAlpha = maskAlpha;
	}

	/**
	 * <p>
	 * Utility method to set the mask parameters for all four color components
	 * at once. If a color's boolean is true, that color component will be
	 * written to the framebuffer. If it's false, it will be masked out.
	 * </p>
	 * <p>
	 * This implies that all true values all normal color writing, and all
	 * falses will block all color writing.
	 * </p>
	 * 
	 * @param red The mask value for red components
	 * @param green The mask value for green components
	 * @param blue The mask value for green components
	 * @param alpha The mask value for alpha components
	 */
	public void setMasks(boolean red, boolean green, boolean blue, boolean alpha) {
		setRedMasked(red);
		setGreenMasked(green);
		setBlueMasked(blue);
		setAlphaMasked(alpha);
	}
}

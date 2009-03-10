package com.ferox.math;


/**
 * A simple class to hold onto the four color components, red, green, blue and alpha.
 * Whenever setting values on a ColorRgba, the components are clamped to be within [0, 1].
 * 
 * @author Michael Ludwig
 *
 */
public class Color {
	private float red;
	private float green;
	private float blue;
	private float alpha;
	
	/** Creates a color <0, 0, 0, 1>. */
	public Color() {
		this(0f, 0f, 0f, 1f);
	}
	
	/** Creates a color <red, green, blue, 1>. */
	public Color(float red, float green, float blue) {
		this(red, green, blue, 1f);
	}
	
	/** Creates a color <red, green, blue, alpha>. */
	public Color(float red, float green, float blue, float alpha) {
		this.set(red, green, blue, alpha);
	}
	
	/** Creates a color that is a copy of other, other can't be null. */
	public Color(Color other) {
		this(other.red, other.green, other.blue, other.alpha);
	}
	
	/** Sets this color's components to be the given rgb values, and an alpha of 1. */
	public void set(float red, float green, float blue) {
		this.set(red, green, blue, 1f);
	}
	
	/** Sets this color's components to be the given rgba values. */
	public void set(float red, float green, float blue, float alpha) {
		this.setRed(red);
		this.setGreen(green);
		this.setBlue(blue);
		this.setAlpha(alpha);
	}
	
	/** Set the red component, clamped to [0, 1]. */
	public void setRed(float red) {
		this.red = Math.max(0f, Math.min(1f, red));
	}
	
	/** Set the green component, clamped to [0, 1]. */
	public void setGreen(float green) {
		this.green = Math.max(0f, Math.min(1f, green));
	}
	
	/** Set the blue component, clamped to [0, 1]. */
	public void setBlue(float blue) {
		this.blue = Math.max(0f, Math.min(1f, blue));
	}
	
	/** Set the alpha component, clamped to [0, 1]. */
	public void setAlpha(float alpha) {
		this.alpha = Math.max(0f, Math.min(1f, alpha));
	}

	/** Get the red component. */
	public float getRed() {
		return this.red;
	}

	/** Get the green component. */
	public float getGreen() {
		return this.green;
	}

	/** Get the blue component. */
	public float getBlue() {
		return this.blue;
	}

	/** Get the alpha component. */
	public float getAlpha() {
		return this.alpha;
	}
	
	/** Fill the given array with the rgba values of this color.  The array can't be null
	 * and it must be four elements in size.  Values are stored as such:
	 * Index	Value
	 *   0		red
	 *   1		green
	 *   2		blue
	 *   3  	alpha */
	public void get(float[] result) throws IllegalArgumentException {
		if (result == null || result.length != 4)
			throw new IllegalArgumentException("Invalid input for get(), can't be null and must have a length of 4: " + result);
		result[0] = this.red;
		result[1] = this.green;
		result[2] = this.blue;
		result[3] = this.alpha;
	}
	
	/** Return true if the float array's components match this 
	 * color's values.  Returns false if the array is null or its
	 * length is not 4.
	 * 
	 * Otherwise, the array is assumed to be laid out like in get(float[]). */
	public boolean equals(float[] color) {
		if (color == null || color.length != 4)
			return false;
		return this.red == color[0] && this.green == color[1] && this.blue == color[2] & this.alpha == color[3];
	}
	
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Color))
			return false;
		if (other == this)
			return true;
		Color that = (Color) other;
		return that.red == this.red && that.green == this.green && that.blue == this.blue && that.alpha == this.alpha;
	}
	
	public int hashCode() {
		int r = Float.floatToIntBits(this.red);
		int g = Float.floatToIntBits(this.green);
		int b = Float.floatToIntBits(this.blue);
		int a = Float.floatToIntBits(this.alpha);
		return r ^ g ^ b ^ a;
	}
}

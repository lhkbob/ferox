package com.ferox.math;

/**
 * A simple class to hold onto the four color components, red, green, blue and
 * alpha. Whenever setting values on a ColorRgba, the components are clamped to
 * be within [0, 1].
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

	/**
	 * Creates a color <red, green, blue, 1>.
	 * 
	 * @param red Red color value, clamped in [0, 1]
	 * @param green Green color value, clamped in [0, 1]
	 * @param blue Blue color value, clamped in [0, 1]
	 */
	public Color(float red, float green, float blue) {
		this(red, green, blue, 1f);
	}

	/**
	 * Creates a color <red, green, blue, alpha>.
	 * 
	 * @param red Red color value, clamped in [0, 1]
	 * @param green Green color value, clamped in [0, 1]
	 * @param blue Blue color value, clamped in [0, 1]
	 * @param alpha Alpha color value, clamped in [0, 1]
	 */
	public Color(float red, float green, float blue, float alpha) {
		this.set(red, green, blue, alpha);
	}

	/**
	 * Creates a color that is a copy of other, other can't be null.
	 * 
	 * @param other Color whose values are copied
	 * @throws NullPointerException if other is null
	 */
	public Color(Color other) throws NullPointerException {
		this(other.red, other.green, other.blue, other.alpha);
	}

	/**
	 * Sets this color's components to be the given rgb values, and an alpha of
	 * 1.
	 * 
	 * @param red Red color value, clamped in [0, 1]
	 * @param green Green color value, clamped in [0, 1]
	 * @param blue Blue color value, clamped in [0, 1]
	 */
	public void set(float red, float green, float blue) {
		this.set(red, green, blue, 1f);
	}

	/**
	 * Sets this color's components to be the given rgba values.
	 * 
	 * @param red Red color value, clamped in [0, 1]
	 * @param green Green color value, clamped in [0, 1]
	 * @param blue Blue color value, clamped in [0, 1]
	 * @param alpha Alpha color value, clamped in [0, 1]
	 */
	public void set(float red, float green, float blue, float alpha) {
		setRed(red);
		setGreen(green);
		setBlue(blue);
		setAlpha(alpha);
	}

	/**
	 * Set the red component, clamped to [0, 1].
	 * 
	 * @param red Red color value, clamped in [0, 1]
	 */
	public void setRed(float red) {
		this.red = Math.max(0f, Math.min(1f, red));
	}

	/**
	 * Set the green component, clamped to [0, 1].
	 * 
	 * @param green Green color value, clamped in [0, 1]
	 */
	public void setGreen(float green) {
		this.green = Math.max(0f, Math.min(1f, green));
	}

	/**
	 * Set the blue component, clamped to [0, 1].
	 * 
	 * @param blue Blue color value, clamped in [0, 1]
	 */
	public void setBlue(float blue) {
		this.blue = Math.max(0f, Math.min(1f, blue));
	}

	/**
	 * Set the alpha component, clamped to [0, 1].
	 * 
	 * @param alpha Alpha color value, clamped in [0, 1]
	 */
	public void setAlpha(float alpha) {
		this.alpha = Math.max(0f, Math.min(1f, alpha));
	}

	/**
	 * Get the red component.
	 * 
	 * @return The red component of this color
	 */
	public float getRed() {
		return red;
	}

	/**
	 * Get the green component.
	 * 
	 * @return The green component of this color
	 */
	public float getGreen() {
		return green;
	}

	/**
	 * Get the blue component.
	 * 
	 * @return The blue component of this color
	 */
	public float getBlue() {
		return blue;
	}

	/**
	 * Get the alpha component.
	 * 
	 * @return The alpha component of this color
	 */
	public float getAlpha() {
		return alpha;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Color)) {
			return false;
		}
		if (other == this) {
			return true;
		}
		Color that = (Color) other;
		return that.red == red && that.green == green && that.blue == blue
						&& that.alpha == alpha;
	}

	@Override
	public int hashCode() {
		int r = Float.floatToIntBits(red);
		int g = Float.floatToIntBits(green);
		int b = Float.floatToIntBits(blue);
		int a = Float.floatToIntBits(alpha);
		return r ^ g ^ b ^ a;
	}

	@Override
	public String toString() {
		return "(Color " + red + ", " + green + ", " + blue + ", " + alpha + ")";
	}
}

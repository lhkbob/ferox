package com.ferox.math;

import java.nio.FloatBuffer;

/**
 * Color3f is a concrete implementation of ReadOnlyColor3f that exposes setters
 * to allow the instance to be mutated.
 * 
 * @author Michael Ludwig
 */
public final class Color3f extends ReadOnlyColor3f {
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    
    private final float[] rgb;
    private final float[] rgbHDR;
    
    /**
     * Create a new Color3f with red, green, and blue values equal to 0.
     */
    public Color3f() {
        rgb = new float[] { 0f, 0f, 0f };
        rgbHDR = new float[] { 0f, 0f, 0f };
    }

    /**
     * Create a new Color3f that copies the color values from <tt>color</tt>
     * initially.
     * 
     * @see #set(ReadOnlyColor3f)
     * @param color The color to copy
     * @throws NullPointerException if color is null
     */
    public Color3f(ReadOnlyColor3f color) {
        this();
        set(color);
    }

    /**
     * Create a new Color3f that uses the given red, green and blue color values
     * initially.
     * 
     * @see #set(float, float, float)
     * @param red The red value
     * @param green The green value
     * @param blue The blue value
     */
    public Color3f(float red, float green, float blue) {
        this();
        set(red, green, blue);
    }

    /**
     * Copy the color values from <tt>color</tt> into this color.
     * 
     * @param color The color to copy
     * @return This color
     * @throws NullPointerException if color is null
     */
    public Color3f set(ReadOnlyColor3f color) {
        return set(color.getRedHDR(), color.getGreenHDR(), color.getBlueHDR());
    }

    /**
     * Set this color to use the given red, green and blue values. This is
     * equivalent to calling {@link #setRed(float)}, {@link #setGreen(float)},
     * and {@link #setBlue(float)} with their appropriate values.
     * 
     * @param red The new red value
     * @param green The new green value
     * @param blue The new blue value
     * @return This color
     */
    public Color3f set(float red, float green, float blue) {
        return setRed(red).
               setGreen(green).
               setBlue(blue);
    }

    /**
     * Set the red component value to <tt>blue</tt>. This value can be greater
     * than 1 and will be unclamped from {@link #getRedHDR()}, but will be
     * clamped to below 1 for {@link #getRed()}. Values below 0 will always be
     * clamped to 0, regardless of dynamic range.
     * 
     * @param red The new red color value
     * @return This color
     */
    public Color3f setRed(float red) {
        return setValue(red, RED);
    }

    /**
     * Set the green component value to <tt>blue</tt>. This value can be greater
     * than 1 and will be unclamped from {@link #getGreenHDR()}, but will be
     * clamped to below 1 for {@link #getGreen()}. Values below 0 will always be
     * clamped to 0, regardless of dynamic range.
     * 
     * @param green The new green color value
     * @return This color
     */
    public Color3f setGreen(float green) {
        return setValue(green, GREEN);
    }

    /**
     * Set the blue component value to <tt>blue</tt>. This value can be greater
     * than 1 and will be unclamped from {@link #getBlueHDR()}, but will be
     * clamped to below 1 for {@link #getBlue()}. Values below 0 will always be
     * clamped to 0, regardless of dynamic range.
     * 
     * @param blue The new blue color value
     * @return This color
     */
    public Color3f setBlue(float blue) {
        return setValue(blue, BLUE);
    }
    
    private Color3f setValue(float v, int i) {
        rgbHDR[i] = Math.max(0f, v);
        rgb[i] = Math.min(rgbHDR[i], 1f);
        return this;
    }

    /**
     * Set the component at <tt>index</tt> to the given value. This value can be
     * greater than 1 and will be returned unclamped the HDR functions, but will
     * be clamped to below 1 for LDR values. Values below 0 will always be
     * clamped to 0, regardless of dynamic range.
     * 
     * @param index The component index to set
     * @param value The color value for the component
     * @return This color
     * @throws IndexOutOfBoundsException if index isn't 0, 1, or 2
     */
    public Color3f set(int index, float value) {
        if (index >= 0 && index < 3)
            return setValue(value, index);
        else
            throw new IndexOutOfBoundsException("Illegal index, must be in [0, 2], not: " + index);
    }

    /**
     * Set this color to the red, green, and blue color values taken from the
     * given array, starting at <tt>offset</tt>. The values can be LDR or HDR,
     * just as in {@link #set(float, float, float)}. This assumes that there are
     * at least three elements left in the array, starting at offset.
     * 
     * @param values The array to take color values from
     * @param offset The offset into values to take the first component from
     * @return This color
     * @throws ArrayIndexOutOfBoundsException if values does not have enough
     *             elements to take 3 color values from
     */
    public Color3f set(float[] values, int offset) {
        return set(values[offset], values[offset + 1], values[offset + 2]);
    }

    /**
     * As {@link #set(float[], int)} but a FloatBuffer is used as a source for
     * float values.
     * 
     * @param values The FloatBuffer to take color values from
     * @param offset The offset into values to take the first component
     * @return This color
     * @throws ArrayIndexOutOfBoundsException if values does not have enough
     *             elements to take 3 color values from
     */
    public Color3f set(FloatBuffer values, int offset) {
        return set(values.get(offset), values.get(offset + 1), values.get(offset + 2));
    }
    
    @Override
    public float getRed() {
        return rgb[RED];
    }

    @Override
    public float getGreen() {
        return rgb[GREEN];
    }

    @Override
    public float getBlue() {
        return rgb[BLUE];
    }

    @Override
    public float getRedHDR() {
        return rgbHDR[RED];
    }

    @Override
    public float getGreenHDR() {
        return rgbHDR[GREEN];
    }

    @Override
    public float getBlueHDR() {
        return rgbHDR[BLUE];
    }
}

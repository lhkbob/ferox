package com.ferox.math;

import java.nio.FloatBuffer;

/**
 * <p>
 * ReadOnlyColor3f is a 3-tuple that stores red, green, and blue color
 * components to represent a color in the classical RGB scheme. It does not
 * support sRGB, although there is nothing stopping you from storing sRGB within
 * the color and performing the math manually.
 * </p>
 * <p>
 * Color component values are normally defined in the range from 0 to 1, where 1
 * represents full saturation of the component for a standard display. So (0, 0,
 * 0) is black and (1, 1, 1) would be white. ReadOnlyColor3f stores color values
 * past 1 so that high-dynamic range colors can be used. However,
 * ReadOnlyColor3f does not perform any tone-mapping so (2, 2, 2) and (3, 1, 5)
 * would both be considered as (1, 1, 1) when not asking for LDR values.
 * </p>
 * <p>
 * It is assumed that systems supporting HDR will use the HDR values and perform
 * tone-mapping manually. The clamping is provided as a convenience for colors
 * where HDR values are meaningless, such as with materials (where a value of 1
 * is the highest physically correct value).
 * </p>
 * <p>
 * It is a common practice to use a {@link ReadOnlyVector4f} when an RGBA color
 * is needed, such as in the lower-level rendering APIs in com.ferox.renderer.
 * </p>
 * 
 * @see Color3f
 * @author Michael Ludwig
 */
public abstract class ReadOnlyColor3f {
    private static final float DEFAULT_FACTOR = .7f;

    /**
     * Convenience function to get a new color that is brighter than this color.
     * This is equivalent to <code>brighter(1.43, null);</code>
     * 
     * @return The brighter color
     */
    public Color3f brighter() {
        return brighter(1f / DEFAULT_FACTOR, null);
    }

    /**
     * Get a brighter version of this color. <tt>result</tt> will hold the
     * brightened color, unless it is null, in which case a new Color3f is
     * created. The <tt>factor</tt> represents the fraction to scale each
     * component by. This uses the HDR color values, so the brighter color might
     * extend into high dynamic range values (i.e. if you brightened white).
     * 
     * @param factor The factor to scale by, must be at least 1
     * @param result The color to store the brightened color
     * @return The brightened color in result, or a new color if result was null
     * @throws IllegalArgumentException if factor is less than 1
     */
    public Color3f brighter(float factor, Color3f result) {
        if (factor < 1f)
            throw new IllegalArgumentException("Brightening factor must be at least 1, not: " + factor);
        
        if (result == null)
            result = new Color3f();
        
        float minBrightness = factor / (255f * factor - 255f);
        float r = getRedHDR();
        float g = getGreenHDR();
        float b = getBlueHDR();
        
        // if the color is black, then the brighter color must be a gray
        if (r == 0 && g == 0 && b == 0)
           return result.set(minBrightness, minBrightness, minBrightness);

        // check for > 0 here so that non-black colors don't have component creep,
        // this is so that dark blue brightens into a brighter blue, without adding
        // any red or green
        if (r > 0 && r < minBrightness) 
            r = minBrightness;
        if (g > 0 && g < minBrightness) 
            g = minBrightness;
        if (b > 0 && b < minBrightness) 
            b = minBrightness;

        return result.set(r * factor, g * factor, b * factor);
    }

    /**
     * Convenience function to get a new color that is darker than this color.
     * This is equivalent to <code>darker(0.7, null);</code>
     * 
     * @return The darker color
     */
    public Color3f darker() {
        return darker(DEFAULT_FACTOR, null);
    }

    /**
     * Get a darker version of this color. <tt>result</tt> will hold the
     * darkened color, unless it is null, in which case a new Color3f is
     * created. The <tt>factor</tt> represents the fraction to scale each
     * component by. This uses the HDR color values, so it might be possible for
     * the darkened color to be equal in low dynamic.
     * 
     * @param factor The factor to scale by, in the range (0, 1]
     * @param result The color to store the darkened color
     * @return The darkened color in result, or a new color if result was null
     * @throws IllegalArgumentException if factor is not in (0, 1]
     */
    public Color3f darker(float factor, Color3f result) {
        if (factor <= 0f || factor > 1f)
            throw new IllegalArgumentException("Darkening factor must be in the range (0, 1], not: " + factor);

        if (result == null)
            result = new Color3f();
        return result.set(Math.max(0f, factor * getRedHDR()),
                          Math.max(0f, factor * getGreenHDR()),
                          Math.max(0f, factor * getBlueHDR()));
    }

    /**
     * Return the clamped color value for the red component. This value will be
     * between 0 and 1, where 1 represents full saturation for the component.
     * This will return 1 if {@link #getRedHDR()} returns a value greater than
     * 1. Only clamping is performed to get this to [0, 1], tone-mapping is not
     * performed.
     * 
     * @return The clamped red value
     */
    public abstract float getRed();

    /**
     * Return the clamped color value for the green component. This value will
     * be between 0 and 1, where 1 represents full saturation for the component.
     * This will return 1 if {@link #getGreenHDR()} returns a value greater than
     * 1. Only clamping is performed to get this to [0, 1], tone-mapping is not
     * performed.
     * 
     * @return The clamped green value
     */
    public abstract float getGreen();

    /**
     * Return the clamped color value for the blue component. This value will be
     * between 0 and 1, where 1 represents full saturation for the component.
     * This will return 1 if {@link #getBlueHDR()} returns a value greater than
     * 1. Only clamping is performed to get this to [0, 1], tone-mapping is not
     * performed.
     * 
     * @return The clamped blue value
     */
    public abstract float getBlue();

    /**
     * Return the unclamped color value for the red component. This value will
     * be at least 0 and has no maximum. Any value higher than 1 represents a
     * high dynamic range value.
     * 
     * @return The unclamped red
     */
    public abstract float getRedHDR();

    /**
     * Return the unclamped color value for the green component. This value will
     * be at least 0 and has no maximum. Any value higher than 1 represents a
     * high dynamic range value.
     * 
     * @return The unclamped green
     */
    public abstract float getGreenHDR();

    /**
     * Return the unclamped color value for the blue component. This value will
     * be at least 0 and has no maximum. Any value higher than 1 represents a
     * high dynamic range value.
     * 
     * @return The unclamped blue
     */
    public abstract float getBlueHDR();
    
    /**
     * Get the clamped color value for <tt>component</tt>. Red is 0, green is 1
     * and blue is 2. The value will be in [0, 1].
     * 
     * @param component The color component to look up
     * @return The clamped color for the given component
     */
    public float get(int component) {
        switch(component) {
        case 0:
            return getRed();
        case 1:
            return getGreen();
        case 2:
            return getBlue();
        default:
            throw new IndexOutOfBoundsException("Component must be between 0 and 2, not: " + component);
        }
    }

    /**
     * Get the unclamped color value for <tt>component</tt>. Red is 0, green is
     * 1 and blue is 2.
     * 
     * @param component The color component to look up
     * @return The unclamped, HDR color for the given component
     */
    public float getHDR(int component) {
        switch(component) {
        case 0:
            return getRedHDR();
        case 1:
            return getGreenHDR();
        case 2:
            return getBlueHDR();
        default:
            throw new IndexOutOfBoundsException("Component must be between 0 and 2, not: " + component);
        }
    }

    /**
     * Copy the clamped color values of this color into <tt>vals</tt> starting
     * at <tt>offset</tt>. It is assumed that vals has 3 indices starting at
     * offset. The color values will be in [0, 1].
     * 
     * @param vals The destination array
     * @param offset The offset into vals
     * @throws ArrayIndexOutOfBoundsException if vals does not have enough space
     *             to store the color
     */
    public void get(float[] vals, int offset) {
        vals[offset + 0] = getRed();
        vals[offset + 1] = getGreen();
        vals[offset + 2] = getBlue();
    }
    
    /**
     * As {@link #get(float[], int)}, but with a FloatBuffer.
     * <tt>offset</tt> is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the color
     */
    public void get(FloatBuffer store, int offset) {
        store.put(offset, getRed());
        store.put(offset + 1, getGreen());
        store.put(offset + 2, getBlue());
    }
    
    /**
     * As {@link #getHDR(float[], int)}, but with a FloatBuffer.
     * <tt>offset</tt> is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the color
     */
    public void getHDR(FloatBuffer store, int offset) {
        store.put(offset, getRedHDR());
        store.put(offset + 1, getGreenHDR());
        store.put(offset + 2, getBlueHDR());
    }

    /**
     * Copy the HDR color values of this color into <tt>vals</tt> starting at
     * <tt>offset</tt>. It is assumed that vals has 3 indices starting at
     * offset.
     * 
     * @param vals The destination array
     * @param offset The offset into vals
     * @throws ArrayIndexOutOfBoundsException if vals does not have enough space
     *             to store the color
     */
    public void getHDR(float[] vals, int offset) {
        vals[offset + 0] = getRedHDR();
        vals[offset + 1] = getGreenHDR();
        vals[offset + 2] = getBlueHDR();
    }

    /**
     * Determine if the two colors are equal. If <tt>asHDR</tt> is true, the HDR
     * color values are compared. If it is false, the color values are first
     * clamped to [0, 1] and then compared.
     * 
     * @param color The other color to compare to
     * @param asHDR True if HDR color ranges are used
     * @return True if they are equal
     */
    public boolean equals(ReadOnlyColor3f color, boolean asHDR) {
        if (color == null)
            return false;
        
        if (asHDR) {
            return Float.compare(getRedHDR(), color.getRedHDR()) == 0 &&
                   Float.compare(getGreenHDR(), color.getGreenHDR()) == 0 &&
                   Float.compare(getBlueHDR(), color.getBlueHDR()) == 0;
        } else {
            return Float.compare(getRed(), color.getRed()) == 0 &&
                   Float.compare(getGreen(), color.getGreen()) == 0 &&
                   Float.compare(getBlue(), color.getBlue()) == 0;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReadOnlyColor3f))
            return false;
        return equals((ReadOnlyColor3f) o, true);
    }
    
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(getRedHDR());
        result += 31 * result + Float.floatToIntBits(getGreenHDR());
        result += 31 * result + Float.floatToIntBits(getBlueHDR());

        return result;
    }
}

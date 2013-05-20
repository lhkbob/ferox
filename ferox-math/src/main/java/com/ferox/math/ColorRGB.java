/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.math;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * <p/>
 * ColorRGB is a 3-tuple that stores red, green, and blue color components to represent a
 * color in the classical RGB scheme. It does not support sRGB, although there is nothing
 * stopping you from storing sRGB within the color and performing the math manually.
 * <p/>
 * Color component values are normally defined in the range from 0 to 1, where 1
 * represents full saturation of the component. So (0, 0, 0) is black and (1, 1, 1) would
 * be white. ColorRGB stores color values past 1 so that high-dynamic range colors can be
 * used. However, ReadOnlyColor3f does not perform any tone-mapping so (2, 2, 2) and (3,
 * 1, 5) would both be considered as (1, 1, 1) when asking for LDR values.
 * <p/>
 * <p/>
 * It is assumed that systems supporting HDR will use the HDR values and perform
 * tone-mapping manually. The clamping is provided as a convenience for colors where HDR
 * values are meaningless, such as with materials (where a value of 1 is the highest
 * physically correct value).
 * <p/>
 * It is a common practice to use a {@link Vector4} when an RGBA color is needed, such as
 * in the lower-level rendering APIs in com.ferox.renderer.
 *
 * @author Michael Ludwig
 */
public final class ColorRGB implements Cloneable {
    private static final double DEFAULT_FACTOR = 0.7;

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;

    private final double[] rgb;
    private final double[] rgbHDR;

    /**
     * Create a new ColorRGB with red, green, and blue values equal to 0.
     */
    public ColorRGB() {
        rgb = new double[] { 0, 0, 0 };
        rgbHDR = new double[] { 0, 0, 0 };
    }

    /**
     * Create a new ColorRGB that copies the color values from <var>color</var>
     * initially.
     *
     * @param color The color to copy
     *
     * @throws NullPointerException if color is null
     * @see #set(ColorRGB)
     */
    public ColorRGB(@Const ColorRGB color) {
        this();
        set(color);
    }

    /**
     * Create a new ColorRGB that uses the given red, green and blue color values
     * initially.
     *
     * @param red   The red value
     * @param green The green value
     * @param blue  The blue value
     *
     * @see #set(double, double, double)
     */
    public ColorRGB(double red, double green, double blue) {
        this();
        set(red, green, blue);
    }

    @Override
    public ColorRGB clone() {
        return new ColorRGB(this);
    }

    /**
     * Get the approximate luminance of this color, clamping the component values to
     * non-HDR values.
     *
     * @return The luminance
     */
    public double luminance() {
        return .3 * red() + .59 * green() + .11 * blue();
    }

    /**
     * Get the approximate luminance of this color, using its HDR component values.
     *
     * @return The HDR luminance
     */
    public double luminanceHDR() {
        return .3 * redHDR() + .59 * greenHDR() + .11 * blueHDR();
    }

    /**
     * Convenience function to brighten this color. This is equivalent to
     * <code>brighter(this, 1.43);</code>
     *
     * @return This color
     */
    public ColorRGB brighter() {
        return brighter(this, 1.0 / DEFAULT_FACTOR);
    }

    /**
     * Compute a brighter version of <var>color</var>, adjusted by the given brightness
     * <var>factor</var> and store it in this color. The <var>factor</var> represents the
     * fraction to scale each component by. This uses the HDR color values, so the
     * brighter color might extend into high dynamic range values (i.e. if you brightened
     * white).
     *
     * @param color  The base color being brightened.
     * @param factor The factor to scale by, must be at least 1
     *
     * @return This color
     *
     * @throws IllegalArgumentException if factor is less than 1
     * @throws NullPointerException     if color is null
     */
    public ColorRGB brighter(@Const ColorRGB color, double factor) {
        if (factor < 1.0) {
            throw new IllegalArgumentException(
                    "Brightening factor must be at least 1, not: " + factor);
        }

        double minBrightness = factor / (255 * factor - 255);
        double r = color.redHDR();
        double g = color.greenHDR();
        double b = color.blueHDR();

        // if the color is black, then the brighter color must be a gray
        if (r == 0 && g == 0 && b == 0) {
            return set(minBrightness, minBrightness, minBrightness);
        }

        // check for > 0 here so that non-black colors don't have component creep,
        // this is so that dark blue brightens into a brighter blue, without adding
        // any red or green
        if (r > 0 && r < minBrightness) {
            r = minBrightness;
        }
        if (g > 0 && g < minBrightness) {
            g = minBrightness;
        }
        if (b > 0 && b < minBrightness) {
            b = minBrightness;
        }

        return set(r * factor, g * factor, b * factor);
    }

    /**
     * Convenience function to darken this color. This is equivalent to <code>darker(this,
     * 0.7);</code>
     *
     * @return This color
     */
    public ColorRGB darker() {
        return darker(this, DEFAULT_FACTOR);
    }

    /**
     * Compute a darker version of <var>color</var>. The <var>factor</var> represents the
     * fraction to scale each component by. This uses the HDR color values, so it might be
     * possible for the darkened color to be equal in the low dynamic range.
     *
     * @param color  The color to darken
     * @param factor The factor to scale by, in the range (0, 1]
     *
     * @return This color
     *
     * @throws IllegalArgumentException if factor is not in (0, 1]
     * @throws NullPointerException     if color is null
     */
    public ColorRGB darker(@Const ColorRGB color, double factor) {
        if (factor <= 0.0 || factor > 1.0) {
            throw new IllegalArgumentException(
                    "Darkening factor must be in the range (0, 1], not: " + factor);
        }

        return set(Math.max(0, factor * color.redHDR()),
                   Math.max(0, factor * color.greenHDR()),
                   Math.max(0, factor * color.blueHDR()));
    }

    /**
     * Copy the color values from <var>color</var> into this color.
     *
     * @param color The color to copy
     *
     * @return This color
     *
     * @throws NullPointerException if color is null
     */
    public ColorRGB set(@Const ColorRGB color) {
        return set(color.redHDR(), color.greenHDR(), color.blueHDR());
    }

    /**
     * Set this color to use the given red, green and blue values. This is equivalent to
     * calling {@link #red(double)}, {@link #green(double)}, and {@link #blue(double)}
     * with their appropriate values.
     *
     * @param red   The new red value
     * @param green The new green value
     * @param blue  The new blue value
     *
     * @return This color
     */
    public ColorRGB set(double red, double green, double blue) {
        return red(red).green(green).blue(blue);
    }

    /**
     * Set the red component value to <var>blue</var>. This value can be greater than 1
     * and will be unclamped for {@link #redHDR()}, but will be clamped to below 1 for
     * {@link #red()}. Values below 0 will always be clamped to 0, regardless of dynamic
     * range.
     *
     * @param red The new red color value
     *
     * @return This color
     */
    public ColorRGB red(double red) {
        return setValue(red, RED);
    }

    /**
     * Set the green component value to <var>blue</var>. This value can be greater than 1
     * and will be unclamped for {@link #greenHDR()}, but will be clamped to below 1 for
     * {@link #green()}. Values below 0 will always be clamped to 0, regardless of dynamic
     * range.
     *
     * @param green The new green color value
     *
     * @return This color
     */
    public ColorRGB green(double green) {
        return setValue(green, GREEN);
    }

    /**
     * Set the blue component value to <var>blue</var>. This value can be greater than 1
     * and will be unclamped for {@link #blueHDR()}, but will be clamped to below 1 for
     * {@link #blue()}. Values below 0 will always be clamped to 0, regardless of dynamic
     * range.
     *
     * @param blue The new blue color value
     *
     * @return This color
     */
    public ColorRGB blue(double blue) {
        return setValue(blue, BLUE);
    }

    private ColorRGB setValue(double v, int i) {
        rgbHDR[i] = Math.max(0, v);
        rgb[i] = Math.min(rgbHDR[i], 1f);
        return this;
    }

    /**
     * Set the component at <var>index</var> to the given value. This value can be greater
     * than 1 and will be returned unclamped the HDR functions, but will be clamped to
     * below 1 for LDR values. Values below 0 will always be clamped to 0, regardless of
     * dynamic range.
     *
     * @param index The component index to set
     * @param value The color value for the component
     *
     * @return This color
     *
     * @throws IndexOutOfBoundsException if index isn't 0, 1, or 2
     */
    public ColorRGB set(int index, double value) {
        if (index >= 0 && index < 3) {
            return setValue(value, index);
        } else {
            throw new IndexOutOfBoundsException(
                    "Illegal index, must be in [0, 2], not: " + index);
        }
    }

    /**
     * Set this color to the red, green, and blue color values taken from the given array,
     * starting at <var>offset</var>. The values can be LDR or HDR, just as in {@link
     * #set(double, double, double)}. This assumes that there are at least three elements
     * left in the array, starting at offset.
     *
     * @param values The array to take color values from
     * @param offset The offset into values to take the first component from
     *
     * @return This color
     *
     * @throws ArrayIndexOutOfBoundsException if values does not have enough elements to
     *                                        take 3 color values from
     */
    public ColorRGB set(double[] values, int offset) {
        return set(values[offset], values[offset + 1], values[offset + 2]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the float[].
     *
     * @param values The array to take color values from
     * @param offset The offset into values to take the first component from
     *
     * @return This color
     *
     * @throws ArrayIndexOutOfBoundsException if values does not have enough elements to
     *                                        take 3 color values from
     */
    public ColorRGB set(float[] values, int offset) {
        return set(values[offset], values[offset + 1], values[offset + 2]);
    }

    /**
     * As {@link #set(double[], int)} but a DoubleBuffer is used as a source for float
     * values.
     *
     * @param values The DoubleBuffer to take color values from
     * @param offset The offset into values to take the first component
     *
     * @return This color
     *
     * @throws ArrayIndexOutOfBoundsException if values does not have enough elements to
     *                                        take 3 color values from
     */
    public ColorRGB set(DoubleBuffer values, int offset) {
        return set(values.get(offset), values.get(offset + 1), values.get(offset + 2));
    }

    /**
     * As {@link #set(double[], int)} but a FloatBuffer is used as a source for float
     * values.
     *
     * @param values The FloatBuffer to take color values from
     * @param offset The offset into values to take the first component
     *
     * @return This color
     *
     * @throws ArrayIndexOutOfBoundsException if values does not have enough elements to
     *                                        take 3 color values from
     */
    public ColorRGB set(FloatBuffer values, int offset) {
        return set(values.get(offset), values.get(offset + 1), values.get(offset + 2));
    }

    /**
     * Return the clamped color value for the red component. This value will be between 0
     * and 1, where 1 represents full saturation for the component. This will return 1 if
     * {@link #redHDR()} returns a value greater than 1. Only clamping is performed to get
     * this to [0, 1], tone-mapping is not performed.
     *
     * @return The clamped red value
     */
    public double red() {
        return rgb[RED];
    }

    /**
     * Return the clamped color value for the green component. This value will be between
     * 0 and 1, where 1 represents full saturation for the component. This will return 1
     * if {@link #greenHDR()} returns a value greater than 1. Only clamping is performed
     * to get this to [0, 1], tone-mapping is not performed.
     *
     * @return The clamped green value
     */
    public double green() {
        return rgb[GREEN];
    }

    /**
     * Return the clamped color value for the blue component. This value will be between 0
     * and 1, where 1 represents full saturation for the component. This will return 1 if
     * {@link #blueHDR()} returns a value greater than 1. Only clamping is performed to
     * get this to [0, 1], tone-mapping is not performed.
     *
     * @return The clamped blue value
     */
    public double blue() {
        return rgb[BLUE];
    }

    /**
     * Return the unclamped color value for the red component. This value will be at least
     * 0 and has no maximum. Any value higher than 1 represents a high dynamic range
     * value.
     *
     * @return The unclamped red
     */
    public double redHDR() {
        return rgbHDR[RED];
    }

    /**
     * Return the unclamped color value for the green component. This value will be at
     * least 0 and has no maximum. Any value higher than 1 represents a high dynamic range
     * value.
     *
     * @return The unclamped green
     */
    public double greenHDR() {
        return rgbHDR[GREEN];
    }

    /**
     * Return the unclamped color value for the blue component. This value will be at
     * least 0 and has no maximum. Any value higher than 1 represents a high dynamic range
     * value.
     *
     * @return The unclamped blue
     */
    public double blueHDR() {
        return rgbHDR[BLUE];
    }

    /**
     * Get the clamped color value for <var>component</var>. Red is 0, green is 1 and blue
     * is 2. The value will be in [0, 1].
     *
     * @param component The color component to look up
     *
     * @return The clamped color for the given component
     */
    public double get(int component) {
        switch (component) {
        case 0:
            return red();
        case 1:
            return green();
        case 2:
            return blue();
        default:
            throw new IndexOutOfBoundsException(
                    "Component must be between 0 and 2, not: " + component);
        }
    }

    /**
     * Get the unclamped color value for <var>component</var>. Red is 0, green is 1 and
     * blue is 2.
     *
     * @param component The color component to look up
     *
     * @return The unclamped, HDR color for the given component
     */
    public double getHDR(int component) {
        switch (component) {
        case 0:
            return redHDR();
        case 1:
            return greenHDR();
        case 2:
            return blueHDR();
        default:
            throw new IndexOutOfBoundsException(
                    "Component must be between 0 and 2, not: " + component);
        }
    }

    /**
     * Copy the clamped color values of this color into <var>vals</var> starting at
     * <var>offset</var>. It is assumed that vals has 3 indices starting at offset. The
     * color values will be in [0, 1].
     *
     * @param vals   The destination array
     * @param offset The offset into vals
     *
     * @throws ArrayIndexOutOfBoundsException if vals does not have enough space to store
     *                                        the color
     */
    public void get(double[] vals, int offset) {
        vals[offset] = red();
        vals[offset + 1] = green();
        vals[offset + 2] = blue();
    }

    /**
     * As {@link #get(double[], int)} but the values are cast into floats.
     *
     * @param vals   The destination array
     * @param offset The offset into vals
     */
    public void get(float[] vals, int offset) {
        vals[offset] = (float) red();
        vals[offset + 1] = (float) green();
        vals[offset + 2] = (float) blue();
    }

    /**
     * As {@link #get(double[], int)}, but with a DoubleBuffer. <var>offset</var> is
     * measured from 0, not the buffer's position.
     *
     * @param store  The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        color
     */
    public void get(DoubleBuffer store, int offset) {
        store.put(offset, red());
        store.put(offset + 1, green());
        store.put(offset + 2, blue());
    }

    /**
     * As {@link #getHDR(double[], int)}, but with a DoubleBuffer. <var>offset</var> is
     * measured from 0, not the buffer's position.
     *
     * @param store  The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        color
     */
    public void getHDR(DoubleBuffer store, int offset) {
        store.put(offset, redHDR());
        store.put(offset + 1, greenHDR());
        store.put(offset + 2, blueHDR());
    }

    /**
     * As {@link #get(double[], int)}, but with a FloatBuffer. <var>offset</var> is
     * measured from 0, not the buffer's position.
     *
     * @param store  The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        color
     */
    public void get(FloatBuffer store, int offset) {
        store.put(offset, (float) red());
        store.put(offset + 1, (float) green());
        store.put(offset + 2, (float) blue());
    }

    /**
     * As {@link #getHDR(double[], int)}, but with a FloatBuffer. <var>offset</var> is
     * measured from 0, not the buffer's position.
     *
     * @param store  The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        color
     */
    public void getHDR(FloatBuffer store, int offset) {
        store.put(offset, (float) redHDR());
        store.put(offset + 1, (float) greenHDR());
        store.put(offset + 2, (float) blueHDR());
    }

    /**
     * Copy the HDR color values of this color into <var>vals</var> starting at
     * <var>offset</var>. It is assumed that vals has 3 indices starting at offset.
     *
     * @param vals   The destination array
     * @param offset The offset into vals
     *
     * @throws ArrayIndexOutOfBoundsException if vals does not have enough space to store
     *                                        the color
     */
    public void getHDR(double[] vals, int offset) {
        vals[offset] = redHDR();
        vals[offset + 1] = greenHDR();
        vals[offset + 2] = blueHDR();
    }

    /**
     * As {@link #getHDR(double[], int)} except the values are cast to floats to store in
     * the array.
     *
     * @param vals   The destination array
     * @param offset THe offset into vals
     */
    public void getHDR(float[] vals, int offset) {
        vals[offset] = (float) redHDR();
        vals[offset + 1] = (float) greenHDR();
        vals[offset + 2] = (float) blueHDR();
    }

    /**
     * Determine if the two colors are equal. If <var>asHDR</var> is true, the HDR color
     * values are compared. If it is false, the color values are first clamped to [0, 1]
     * and then compared.
     *
     * @param color The other color to compare to
     * @param asHDR True if HDR color ranges are used
     *
     * @return True if they are equal
     */
    public boolean equals(@Const ColorRGB color, boolean asHDR) {
        if (color == null) {
            return false;
        }

        if (asHDR) {
            return Double.compare(redHDR(), color.redHDR()) == 0 &&
                   Double.compare(greenHDR(), color.greenHDR()) == 0 &&
                   Double.compare(blueHDR(), color.blueHDR()) == 0;
        } else {
            return Double.compare(red(), color.red()) == 0 &&
                   Double.compare(green(), color.green()) == 0 &&
                   Double.compare(blue(), color.blue()) == 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ColorRGB && equals((ColorRGB) o, true);
    }

    @Override
    public int hashCode() {
        long result = 17;

        result += 31 * result + Double.doubleToLongBits(redHDR());
        result += 31 * result + Double.doubleToLongBits(greenHDR());
        result += 31 * result + Double.doubleToLongBits(blueHDR());

        return (int) (((result & 0xffffffff00000000L) >> 32) ^
                      (result & 0x00000000ffffffffL));
    }
}

package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.entreri.ColorRGBProperty;
import com.ferox.math.entreri.ColorRGBProperty.DefaultColor;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Unmanaged;

/**
 * <p>
 * Light is an abstract class that Components representing lights within a 3D
 * scene should extend from. Light does not enforce any particular rules on how
 * a light is described, except that it has a {@link #getColor() color}. The
 * Light type exists so that the myriad types of lights do not need to repeat
 * this definition.
 * </p>
 * <p>
 * Additionally, the colors held by Light components should use the HDR values
 * stored in the returned {@link ReadOnlyColor3f}'s. This is because the color
 * of a light can be ultra-bright, going past the conventional limit of 1 for a
 * color component. Support for the HDR values when rendering is dependent on
 * the rendering framework, however.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of light
 */
public abstract class Light<T extends Light<T>> extends ComponentData<T> {
    @DefaultColor(red = 0.2, green = 0.2, blue = 0.2)
    private ColorRGBProperty color;

    @Unmanaged
    private final ColorRGB cache = new ColorRGB();

    protected Light() {}

    /**
     * Return the color of this Light. The returned ColorRGB instance is reused
     * by this Light instance so it should be cloned before changing which
     * Component is referenced.
     * 
     * @return The color of this Light
     */
    public final @Const
    ColorRGB getColor() {
        return cache;
    }

    /**
     * Set the color of this Light. The color values in <tt>color</tt> are
     * copied into an internal instance, so any future changes to <tt>color</tt>
     * will not affect this Component.
     * 
     * @param color The new color
     * @return This light for chaining purposes
     * @throws NullPointerException if color is null
     */
    @SuppressWarnings("unchecked")
    public final T setColor(@Const ColorRGB color) {
        if (color == null) {
            throw new NullPointerException("Color cannot be null");
        }
        cache.set(color);
        this.color.set(color, getIndex());
        return (T) this;
    }

    @Override
    protected void onSet(int index) {
        color.get(index, cache);
    }
}

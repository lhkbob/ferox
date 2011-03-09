package com.ferox.scene;

import com.ferox.entity.TypedComponent;
import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyColor4f;

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
 * stored in the returned {@link ReadOnlyColor4f}'s. This is because the color
 * of a light can be ultra-bright, going past the conventional limit of 1 for a
 * color component. Support for the HDR values when rendering is dependent on
 * the rendering framework, however.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of light
 */
public abstract class Light<T extends Light<T>> extends TypedComponent<T> {
    private final Color4f color;

    /**
     * Default constructor that sets the light's color to white.
     */
    protected Light() {
        super(null, false);
        color = new Color4f(1f, 1f, 1f);
    }
    
    /**
     * Override the cloning constructor to only operate on an
     * actual clone. Use the default constructor in subclasses when
     * a clone is not needed
     * 
     * @param clone The Light of type T to clone
     * @throws NullPointerException if clone is null
     */
    protected Light(T clone) {
        super(clone, true);
        color = new Color4f(clone.color);
    }

    /**
     * Return the color of this Light. The returned reference will not change,
     * although the color's values might in response to
     * {@link #setColor(ReadOnlyColor4f)}.
     * 
     * @return The color of this Light
     */
    public ReadOnlyColor4f getColor() {
        return color;
    }

    /**
     * Set the color of this Light. The color values in <tt>color</tt> are
     * copied into an internal instance, so any future changes to <tt>color</tt>
     * will not affect this Component.
     * 
     * @param color The new color
     * @return The new version of this Light, via {@link #notifyChange()}
     * @throws NullPointerException if color is null
     */
    public int setColor(ReadOnlyColor4f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        this.color.set(color);
        return notifyChange();
    }
}
